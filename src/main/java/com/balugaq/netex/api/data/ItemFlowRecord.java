package com.balugaq.netex.api.data;

import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.StackUtils;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ItemFlowRecord {
    final Map<ItemStack, List<TransportAction>> actions = new ConcurrentHashMap<>();
    long actionsCount = 0;
    long lastChangeTime = System.currentTimeMillis();

    public void gc() {
        if (actionsCount > Networks.getConfigManager().getRecordGCThreshold()) {
            actions.clear();
        }
    }

    public void forceGC() {
        actions.clear();
    }

    public void addAction(Location accessor, @NotNull ItemStack before, int after) {
        int change = before.getAmount() - after;
        if (change == 0) {
            return;
        }

        lastChangeTime = System.currentTimeMillis();
        actionsCount++;

        ItemStack key = StackUtils.asKey(before);
        List<TransportAction> list = actions.computeIfAbsent(key, k -> new ArrayList<>());

        TransportAction action = new TransportAction(accessor, change, System.currentTimeMillis());
        list.add(action);
    }

    public void addAction(Location accessor, @NotNull ItemRequest request) {
        if (request.getReceivedAmount() == 0) {
            return;
        }

        lastChangeTime = System.currentTimeMillis();
        actionsCount++;

        ItemStack key = StackUtils.getAsQuantity(request.getItemStack(), 1);
        List<TransportAction> list = actions.computeIfAbsent(key, k -> new ArrayList<>());

        TransportAction action =
            new TransportAction(accessor, -request.getReceivedAmount(), System.currentTimeMillis());
        list.add(action);
    }

    public record TransportAction(Location accessor, long amount, long milliSecond) {
    }
}
