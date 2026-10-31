package com.balugaq.netex.utils;

import com.ytdd9527.networksexpansion.utils.ReflectionUtil;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;

public class VersionUtil {
    public static Inventory getTopInventory(InventoryEvent event) {
        return (Inventory) ReflectionUtil.invokeMethod(ReflectionUtil.invokeMethod(event, "getView"), "getTopInventory");
    }
}
