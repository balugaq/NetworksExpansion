package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MenuShells {

    @FunctionalInterface
    public interface CanOpen {
        boolean test(@NotNull Block block, @NotNull Player player);
    }

    @FunctionalInterface
    public interface TransportSlots {
        int[] get(@NotNull ItemTransportFlow flow);
    }

    @FunctionalInterface
    public interface OnReady {
        void ready(@NotNull BlockMenu menu, @NotNull Block block);
    }

    private MenuShells() {
    }

    public static void create(
            @NotNull SpecialSlimefunItem owner,
            int size,
            int @NotNull [] backgroundSlots,
            @Nullable ItemStack backgroundItem,
            @NotNull CanOpen canOpen,
            @NotNull TransportSlots transport,
            @NotNull OnReady onReady) {
        new BlockMenuPreset(owner.getId(), owner.getItemName()) {
            @Override
            public void init() {
                setSize(size);
                if (backgroundItem != null) {
                    for (int slot : backgroundSlots) {
                        addItem(slot, backgroundItem.clone());
                    }
                } else {
                    drawBackground(backgroundSlots);
                }
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return canOpen.test(block, player);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(@NotNull ItemTransportFlow flow) {
                return transport.get(flow);
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                for (int slot : backgroundSlots) {
                    menu.addMenuClickHandler(slot, (p, s, i, a) -> false);
                }
                onReady.ready(menu, block);
            }
        };
    }

    public static void create(
            @NotNull SpecialSlimefunItem owner,
            int size,
            int @NotNull [] backgroundSlots,
            @NotNull CanOpen canOpen,
            @NotNull TransportSlots transport,
            @NotNull OnReady onReady) {
        create(owner, size, backgroundSlots, null, canOpen, transport, onReady);
    }

    @NotNull
    public static int[] emptyTransport(@NotNull ItemTransportFlow flow) {
        return new int[0];
    }
}
