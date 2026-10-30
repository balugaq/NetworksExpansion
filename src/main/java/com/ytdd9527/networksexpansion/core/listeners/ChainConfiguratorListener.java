package com.ytdd9527.networksexpansion.core.listeners;

import com.ytdd9527.networksexpansion.core.machines.AbstractChainMachine;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import com.ytdd9527.networksexpansion.implementation.items.cell.ChainConfigurator;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChainConfiguratorListener implements Listener {
    @EventHandler
    public void onLeftClickBlock(@NotNull PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(held);
        if (sfItem != ExpansionItems.CHAIN_CONFIGURATOR) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        Location location = block.getLocation();
        AbstractChainMachine machine = AbstractChainMachine.machineAt(location);
        if (machine == null) {
            return;
        }
        if (!Slimefun.getProtectionManager().hasPermission(player, block, Interaction.INTERACT_BLOCK)) {
            return;
        }
        e.setCancelled(true);
        ChainConfigurator.openSelect(player, location, held);
    }
}
