package com.ytdd9527.networksexpansion.implementation.machines.ae.core.item;

import org.bukkit.inventory.ItemStack;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A {@link Map} whose keys are {@link ItemStack}s compared by their canonical
 * {@link ItemKey} identity (material + full NBT, ignoring stack size).
 */
public class ItemHashMap<V> implements Map<ItemStack, V> {

    private final Map<ItemKey, V> map;

    public ItemHashMap() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof ItemStack) {
            return map.containsKey(new ItemKey((ItemStack) key));
        }
        if (key instanceof ItemKey itemKey) {
            return map.containsKey(itemKey);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key instanceof ItemStack) {
            return map.get(new ItemKey((ItemStack) key));
        }
        return null;
    }

    public V getKey(ItemKey key) {
        return map.get(key);
    }

    @Override
    public V put(ItemStack key, V value) {
        return map.put(new ItemKey(key), value);
    }

    public V putKey(ItemKey key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if (key instanceof ItemStack) {
            return map.remove(new ItemKey((ItemStack) key));
        }
        return null;
    }

    public V removeKey(ItemKey key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends ItemStack, ? extends V> m) {
        for (Map.Entry<? extends ItemStack, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<ItemStack> keySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<ItemStack> iterator() {
                return new Iterator<>() {
                    private final Iterator<ItemKey> wrapperIterator = map.keySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return wrapperIterator.hasNext();
                    }

                    @Override
                    public ItemStack next() {
                        return wrapperIterator.next().getItemStack();
                    }

                    @Override
                    public void remove() {
                        wrapperIterator.remove();
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean contains(Object o) {
                return ItemHashMap.this.containsKey(o);
            }

            @Override
            public boolean remove(Object o) {
                return ItemHashMap.this.remove(o) != null;
            }
        };
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<ItemStack, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Map.Entry<ItemStack, V>> iterator() {
                return new Iterator<>() {
                    private final Iterator<Map.Entry<ItemKey, V>> entryIterator = map.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<ItemStack, V> next() {
                        Map.Entry<ItemKey, V> entry = entryIterator.next();
                        return new AbstractMap.SimpleEntry<>(entry.getKey().getItemStack(), entry.getValue());
                    }

                    @Override
                    public void remove() {
                        entryIterator.remove();
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                    if (entry.getKey() instanceof ItemStack) {
                        ItemKey wrapper = new ItemKey((ItemStack) entry.getKey());
                        return map.containsKey(wrapper) && Objects.equals(map.get(wrapper), entry.getValue());
                    }
                }
                return false;
            }

            @Override
            public boolean remove(Object o) {
                if (o instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                    if (entry.getKey() instanceof ItemStack) {
                        ItemKey wrapper = new ItemKey((ItemStack) entry.getKey());
                        if (Objects.equals(map.get(wrapper), entry.getValue())) {
                            map.remove(wrapper);
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public Set<Map.Entry<ItemKey, V>> keyEntrySet() {
        return map.entrySet();
    }

    public V getOrDefault(ItemKey key, V defaultValue) {
        V v = getKey(key);
        return (v != null || containsKey(key)) ? v : defaultValue;
    }

    @Override
    public String toString() {
        return map.entrySet().stream()
            .map(entry -> entry.getKey().getItemStack() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
    }
}