package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.template;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ItemKeyTplIdBridge {

    private final ConcurrentHashMap<ItemKey, Long> keyToTplId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ItemKey> tplIdToKey = new ConcurrentHashMap<>();
    private final ItemTemplateRegistry registry;

    public ItemKeyTplIdBridge(ItemTemplateRegistry registry) {
        this.registry = registry;
    }

    public long getOrResolve(@NotNull ItemKey itemKey) {
        Long cached = keyToTplId.get(itemKey);
        if (cached != null) {
            return cached;
        }
        long tplId = registry.getOrRegister(itemKey.getItemStack());
        keyToTplId.put(itemKey, tplId);
        tplIdToKey.put(tplId, itemKey);
        return tplId;
    }

    @Nullable
    public ItemKey resolveKey(long tplId) {
        ItemKey cached = tplIdToKey.get(tplId);
        if (cached != null) {
            return cached;
        }
        ItemStack itemStack = registry.resolveItem(tplId);
        if (itemStack == null) {
            return null;
        }
        ItemKey key = new ItemKey(itemStack);
        keyToTplId.put(key, tplId);
        tplIdToKey.put(tplId, key);
        return key;
    }

    public void preload(Collection<Long> tplIds) {
        for (long tplId : tplIds) {
            if (!tplIdToKey.containsKey(tplId)) {
                resolveKey(tplId);
            }
        }
    }
}