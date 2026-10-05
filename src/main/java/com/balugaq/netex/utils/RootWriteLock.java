package com.balugaq.netex.utils;

/**
 * 将量子存储设置物品和 root getItemStack0 强制加锁，使得他们在同一 channel 上，并及时刷新root，以避免刷物。
 *
 * fix item dupe <a href="https://b23.tv/BV1ZMXuBpEAz">Bilibili</a>
 *
 * @author balugaq
 */
public class RootWriteLock {
    private static final Object lock = new Object();
    public static Object get() {
        return lock;
    }
}
