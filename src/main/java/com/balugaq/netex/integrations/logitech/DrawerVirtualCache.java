package com.balugaq.netex.integrations.logitech;

import com.balugaq.netex.api.data.ItemContainer;
import com.balugaq.netex.api.data.StorageUnitData;
import com.ytdd9527.networksexpansion.implementation.machines.unit.NetworksDrawer;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 网络抽屉的虚拟量子缓存：注入 {@link NetworkQuantumStorage#getCaches()} 后，
 * LogiTech2 的量子代理适配器会把它当作一台量子储存读写——
 * 数量读写实时转发到抽屉 {@link ItemContainer}（volatile int + synchronized），零物品搬运。
 * 与元件网络同为实读实写模型：抽屉还会被网络路由等其它途径修改，
 * 只有实时读账本、写入按差额增减才不会吞掉这些变更。
 * 只持有抽屉坐标，{@link NetworksDrawer#getStorageData} 每次现取——
 * 区块未加载时读 0，加载后自愈，重启重放不依赖加载顺序。
 * 反射解析器由 {@link CellVirtualCache#preWarmResolvers()} 钉到 QuantumCache 上。
 */
public class DrawerVirtualCache extends QuantumCache {

    private static final long VIRTUAL_Y_BASE = -1_000_001_000L;
    private static final long VIRTUAL_Y_RANGE = 1_000_000_000L;

    private final Location drawerLocation;
    private final ItemStack sample;

    private DrawerVirtualCache(@NotNull Location drawerLocation, @NotNull ItemStack sample) {
        super(sample, 0, Long.MAX_VALUE, false, false);
        this.drawerLocation = drawerLocation;
        this.sample = sample;
    }

    @Nullable
    private StorageUnitData drawerData() {
        return NetworksDrawer.getStorageData(drawerLocation);
    }

    @Nullable
    private ItemContainer findContainer() {
        StorageUnitData data = drawerData();
        if (data == null) {
            return null;
        }
        for (ItemContainer ic : data.getStoredItemsDirectly()) {
            if (ic.isSimilar(sample)) {
                return ic;
            }
        }
        return null;
    }

    @Override
    public long getAmount() {
        ItemContainer ic = findContainer();
        return ic == null ? 0 : ic.getAmount();
    }

    @Override
    public long getAmountLong() {
        return getAmount();
    }

    @Override
    public int getAmountInt() {
        return (int) Math.min(getAmount(), Integer.MAX_VALUE);
    }

    @Override
    public synchronized void setAmount(long newAmount) {
        ItemContainer container = findContainer();
        if (container == null) {
            return;
        }
        StorageUnitData data = drawerData();
        long max = data == null ? 0 : data.getSizeType().getEachMaxSize();
        long target = Math.min(newAmount, max);
        long current = container.getAmount();
        long delta = target - current;
        if (delta > 0) {
            container.addAmount((int) Math.min(delta, Integer.MAX_VALUE));
        } else if (delta < 0) {
            container.removeAmount((int) Math.min(-delta, Integer.MAX_VALUE));
        }
    }

    @Override
    public synchronized void setAmount(int newAmount) {
        setAmount((long) newAmount);
    }

    @Override
    public long getLimitLong() {
        StorageUnitData data = drawerData();
        return data == null ? 0 : data.getSizeType().getEachMaxSize();
    }

    @NotNull
    public static Location virtualLocation(@NotNull Location drawerLocation, @NotNull ItemStack sample) {
        long combined = CellVirtualCache.stableLocationHash(drawerLocation) * 1_000_003L
            + CellVirtualCache.stableItemHash(sample);
        return new Location(
            drawerLocation.getWorld(),
            drawerLocation.getX(),
            VIRTUAL_Y_BASE - Math.floorMod(combined, VIRTUAL_Y_RANGE),
            drawerLocation.getZ()
        );
    }

    public static void inject(@NotNull Location drawerLocation, @NotNull ItemStack sample) {
        CellVirtualCache.preWarmResolvers();
        Location virtual = virtualLocation(drawerLocation, sample);
        Map<Location, QuantumCache> caches = NetworkQuantumStorage.getCaches();
        if (!caches.containsKey(virtual)) {
            caches.put(virtual, new DrawerVirtualCache(drawerLocation, sample));
        }
    }

    public static void removeDrawer(@NotNull Location drawerLocation) {
        NetworkQuantumStorage.getCaches().values()
                .removeIf(cache -> cache instanceof DrawerVirtualCache virtual
                        && virtual.drawerLocation.equals(drawerLocation));
    }
}
