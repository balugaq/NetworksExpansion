package io.github.sefiraat.networks.network.barrel;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.StackUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkStorage extends BarrelIdentity {
    public NetworkStorage(@NotNull Location location, ItemStack itemStack, long amount) {
        super(location, itemStack, amount, amount, BarrelType.NETWORKS);
    }

    public NetworkStorage(@NotNull Location location, ItemStack itemStack, long amount, long limit) {
        super(location, itemStack, amount, limit, BarrelType.NETWORKS);
    }

    @Override
    @Nullable
    public ItemStack requestItem(@NotNull ItemRequest itemRequest) {
        final BlockMenu blockMenu = StorageCacheUtils.getMenu(this.getLocation());

        if (blockMenu == null) {
            return null;
        }

        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        // 快照一致性校验：getItemStack0/getItemStacksBatch0 用桶扫描时的快照物品匹配请求，
        // 而本方法读取的是活缓存。若两次访问之间存储被取空并改存了其他物品
        // （root 生命周期为一刻，同一刻内完全可能发生），直接扣量会取出错误物品。
        // 校验失败直接放弃本次取物（此时尚未扣量，无损），由外层继续尝试其他桶。
        final ItemStack cacheItem = cache.getItemStack();
        if (cacheItem != null && !StackUtils.itemsMatch(itemRequest.getItemStack(), cacheItem)) {
            return null;
        }

        return NetworkQuantumStorage.getItemStack(cache, blockMenu, itemRequest.getAmount());
    }

    @Override
    public void depositItemStack(ItemStack @NotNull [] itemsToDeposit) {
        if (StorageCacheUtils.getSfItem(this.getLocation()) instanceof NetworkQuantumStorage) {
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(this.getLocation());
            if (blockMenu == null) {
                return;
            }
            final QuantumCache cache = NetworkQuantumStorage.getCaches().get(this.getLocation());
            if (cache != null) {
                NetworkQuantumStorage.tryInputItem(blockMenu.getLocation(), itemsToDeposit, cache);
            }
        }
    }

    @Override
    public int[] getInputSlot() {
        return new int[]{NetworkQuantumStorage.INPUT_SLOT};
    }

    @Override
    public int[] getOutputSlot() {
        return new int[]{NetworkQuantumStorage.OUTPUT_SLOT};
    }
}
