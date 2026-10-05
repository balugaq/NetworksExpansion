/**
 * MIT License
 *
 * Copyright (c) 2024 Ddggdd135
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An ItemStack-keyed map backed by an internal {@link ConcurrentHashMap}.
 * <p>
 * Derived from SlimeAE's ItemHashMap (MIT License, see licenses/MIT.md),
 *
 * @author Ddggdd135
 * @author ytdd9526
 */
@NullMarked
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
        if (key instanceof ItemKey itemKey) {
            return map.get(itemKey);
        }
        return null;
    }

    public @Nullable V getKey(ItemKey key) {
        return map.get(key);
    }

    @Override
    public @Nullable V put(ItemStack key, V value) {
        return map.put(new ItemKey(key), value);
    }

    public @Nullable V putKey(ItemKey key, V value) {
        return map.put(key, value);
    }

    @Override
    public @Nullable V remove(Object key) {
        if (key instanceof ItemStack) {
            return map.remove(new ItemKey((ItemStack) key));
        }
        if (key instanceof ItemKey itemKey) {
            return map.remove(itemKey);
        }
        return null;
    }

    public @Nullable  removeKey(ItemKey key) {
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

    public Map<ItemKey, V> keyedView() {
        return Collections.unmodifiableMap(map);
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
