package com.balugaq.netex.integrations.logitech;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.utils.ReflectionUtil;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * 元件网络的虚拟量子缓存：注入 {@link NetworkQuantumStorage#getCaches()} 后，
 * LogiTech2 的量子代理适配器会把它当作一台量子储存读写——
 * 数量读写实时转发到驱动器元件账本（O(1) 键控 + long 算术），零物品搬运。
 * LogiTech2 反射按运行时类解析并全局缓存 Method：首次注入前先用真 QuantumCache
 * 假体预热解析器，把缓存钉到 QuantumCache 声明的方法上，避免与真量子储存混用时
 * 解析落错类导致适配器累计报错自动注销。
 */
public class CellVirtualCache extends QuantumCache {

    private static final long VIRTUAL_Y_BASE = -1_000_000L;
    private static final long VIRTUAL_Y_RANGE = 1_000_000_000L;

    private static volatile boolean resolversPrewarmed = false;

    private final Location driveLocation;
    private final ItemKey itemKey;

    private CellVirtualCache(@NotNull Location driveLocation, @NotNull ItemKey itemKey) {
        super(itemKey.getItemStack(), 0, Long.MAX_VALUE, false, false);
        this.driveLocation = driveLocation;
        this.itemKey = itemKey;
    }

    @Override
    public long getAmount() {
        return realAmount();
    }

    @Override
    public long getAmountLong() {
        return realAmount();
    }

    @Override
    public int getAmountInt() {
        return (int) Math.min(realAmount(), Integer.MAX_VALUE);
    }

    @Override
    public synchronized void setAmount(long newAmount) {
        long current = realAmount();
        long delta = newAmount - current;
        if (delta > 0) {
            pushToCells(delta);
        } else if (delta < 0) {
            pullFromCells(-delta);
        }
    }

    @Override
    public synchronized void setAmount(int newAmount) {
        setAmount((long) newAmount);
    }

    @Override
    public long getLimitLong() {
        return realAmount() + safeRemaining();
    }

    private long realAmount() {
        BlockMenu menu = StorageCacheUtils.getMenu(driveLocation);
        if (menu == null) {
            return 0;
        }
        long total = 0;
        for (CellHandle cell : CellDrive.getStorage().getCells(menu)) {
            total += cell.getAmount(itemKey);
        }
        return total;
    }

    private long safeRemaining() {
        BlockMenu menu = StorageCacheUtils.getMenu(driveLocation);
        if (menu == null) {
            return 0;
        }
        long remaining = 0;
        for (CellHandle cell : CellDrive.getStorage().getCells(menu)) {
            long cap = cell.receiveCapacity(itemKey);
            if (cap == 0 && cell.canReceiveItem(itemKey)) {
                cap = 1;
            }
            remaining += cap;
        }
        return remaining;
    }

    private void pushToCells(long amount) {
        BlockMenu menu = StorageCacheUtils.getMenu(driveLocation);
        if (menu == null) {
            return;
        }
        long remaining = amount;
        for (CellHandle cell : CellDrive.getStorage().getCells(menu)) {
            if (remaining <= 0) {
                break;
            }
            int pushed = cell.pushItem(itemKey, (int) Math.min(remaining, Integer.MAX_VALUE));
            remaining -= pushed;
        }
    }

    private void pullFromCells(long amount) {
        BlockMenu menu = StorageCacheUtils.getMenu(driveLocation);
        if (menu == null) {
            return;
        }
        long remaining = amount;
        for (CellHandle cell : CellDrive.getStorage().getCells(menu)) {
            if (remaining <= 0) {
                break;
            }
            long taken = cell.takeItemAmount(itemKey, remaining);
            remaining -= taken;
        }
    }

    @NotNull
    public static Location virtualLocation(@NotNull Location driveLocation, @NotNull ItemKey itemKey) {
        long combined = stableLocationHash(driveLocation) * 1_000_003L + stableItemHash(itemKey.getItemStack());
        return new Location(
            driveLocation.getWorld(),
            driveLocation.getX(),
            VIRTUAL_Y_BASE - Math.floorMod(combined, VIRTUAL_Y_RANGE),
            driveLocation.getZ()
        );
    }

    public static long stableLocationHash(@NotNull Location location) {
        World world = location.getWorld();
        long h = (world != null ? world.getName().hashCode() : 0L) * 1_000_000_007L;
        h = h * 31 + location.getBlockX();
        h = h * 31 + location.getBlockY();
        h = h * 31 + location.getBlockZ();
        return h;
    }

    public static long stableItemHash(@NotNull ItemStack template) {
        byte[] bytes = template.serializeAsBytes();
        return bytes.length * 1_000_003L + Arrays.hashCode(bytes);
    }

    public static void inject(@NotNull Location driveLocation, @NotNull ItemKey itemKey) {
        preWarmResolvers();
        Location virtual = virtualLocation(driveLocation, itemKey);
        Map<Location, QuantumCache> caches = NetworkQuantumStorage.getCaches();
        if (!caches.containsKey(virtual)) {
            caches.put(virtual, new CellVirtualCache(driveLocation, itemKey));
        }
    }

    static void preWarmResolvers() {
        if (resolversPrewarmed) {
            return;
        }
        resolversPrewarmed = true;
        try {
            Class<?> storages = ReflectionUtil.getClass(
                "me.matl114.logitech.core.Cargo.Storages",
                "me.matl114.logitech.SlimefunItem.Cargo.Storages");
            if (storages == null) {
                return;
            }
            Field adapterField = storages.getDeclaredField("NTWSTORAGE_PROXY");
            adapterField.setAccessible(true);
            Object adapter = adapterField.get(null);
            if (adapter == null) {
                return;
            }
            Location dummy = new Location(Bukkit.getWorlds().get(0), 0, -2_100_000_000, 0);
            QuantumCache probe = new QuantumCache(new ItemStack(Material.STONE), 1, 16, false, false);
            Map<Location, QuantumCache> caches = NetworkQuantumStorage.getCaches();
            caches.put(dummy, probe);
            try {
                invokeAdapter(adapter, "setAmount", new Class[]{Location.class, long.class}, dummy, 1L);
                invokeAdapter(adapter, "getAmount", new Class[]{Location.class}, dummy);
                invokeAdapter(adapter, "getMaxAmount", new Class[]{Location.class}, dummy);
                invokeAdapter(adapter, "getItemStack", new Class[]{Location.class}, dummy);
            } finally {
                caches.remove(dummy);
            }
        } catch (Throwable e) {
            com.balugaq.netex.utils.Debug.trace(e, "预热 LogiTech2 量子代理反射解析");
        }
    }

    private static void invokeAdapter(
        @NotNull Object adapter, @NotNull String name, @NotNull Class<?>[] params, @NotNull Object... args)
        throws ReflectiveOperationException {
        Method method = adapter.getClass().getMethod(name, params);
        method.invoke(adapter, args);
    }
}
