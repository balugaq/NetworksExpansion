package com.ytdd9527.networksexpansion.core.items.machines;

import com.balugaq.netex.api.enums.CraftType;
import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.interfaces.CraftTyped;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlueprintEncoder extends NetworkObject implements CraftTyped {
    private static final int[] BACKGROUND = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 15, 17, 18, 20, 24, 25, 27, 28, 29, 33, 36, 37, 38, 39, 40, 41,
        42, 43, 44
    };
    private static final int[] RECIPE_SLOTS = new int[]{12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int[] BLUEPRINT_BACK = new int[]{10, 28};
    private static final int BLANK_BLUEPRINT_SLOT = 19;
    private static final int ENCODE_SLOT = 16;
    private static final int OUTPUT_SLOT = 34;
    private static final int ITEM_TARGET_DESC_SLOT = 26;
    private static final int ITEM_TARGET_SLOT = 35;
    private static final int CHARGE_COST = 2000;

    public BlueprintEncoder(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ENCODER);
        for (int recipeSlot : RECIPE_SLOTS) {
            this.getSlotsToDrop().add(recipeSlot);
        }
        this.getSlotsToDrop().add(BLANK_BLUEPRINT_SLOT);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND);
                drawBackground(Icon.BLUEPRINT_BACK_STACK, BLUEPRINT_BACK);

                addItem(ENCODE_SLOT, Icon.ENCODE_STACK, (player, i, itemStack, clickAction) -> false);
                addItem(ITEM_TARGET_DESC_SLOT, Icon.ITEM_TARGET_DESC_STACK, (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block b) {
                menu.addMenuClickHandler(ENCODE_SLOT, (player, s, itemStack, clickAction) -> {
                    int times = clickAction.isShiftClicked() ? 64 : 1;
                    for (int i = 0; i < times; i++) {
                        if (!tryEncode(player, menu)) {
                            break;
                        }
                    }
                    return false;
                });
                var fix = menu.getItemInSlot(ITEM_TARGET_SLOT);
                if (StackUtils.itemsMatch(fix, ChestMenuUtils.getBackground())) {
                    menu.replaceExistingItem(ITEM_TARGET_SLOT, null);
                }
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (this.getSlimefunItem().canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }

                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack itemStack) {
                if (flow == ItemTransportFlow.WITHDRAW) return new int[]{OUTPUT_SLOT};

                List<Integer> slots = new ArrayList<>();
                if (StackUtils.itemsMatch(itemStack, menu.getItemInSlot(BLANK_BLUEPRINT_SLOT))) {
                    slots.add(BLANK_BLUEPRINT_SLOT);
                }

                for (int slot : RECIPE_SLOTS) {
                    if (StackUtils.itemsMatch(itemStack, menu.getItemInSlot(slot))) {
                        slots.add(slot);
                    }
                }
                return slots.stream().mapToInt(Integer::intValue).toArray();
            }
        };
    }

    public boolean tryEncode(@NotNull Player player, @NotNull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.NO_NETWORK_FOUND);
            player.sendMessage(Lang.getString("messages.feedback.no_network_found"));
            return false;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        final long networkCharge = root.getRootPower();

        if (networkCharge < CHARGE_COST) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.not_enough_power"));
            sendFeedback(blockMenu.getLocation(), FeedbackType.NOT_ENOUGH_POWER);
            return false;
        }

        ItemStack blueprint = blockMenu.getItemInSlot(BLANK_BLUEPRINT_SLOT);

        if (blueprint == null || blueprint.getType() == Material.AIR) {
            ItemStack bp = NetworkSlimefunItems.CRAFTING_BLUEPRINT.getItem();
            blueprint = root.getItemStack0(blockMenu.getLocation(), new ItemRequest(bp, bp.getMaxStackSize()));
            if (blueprint == null) {
                player.sendMessage(Lang.getString("messages.feedback.no_blueprint_found"));
                sendFeedback(blockMenu.getLocation(), FeedbackType.NO_BLUEPRINT_FOUND);
                return false;
            }
            blockMenu.replaceExistingItem(BLANK_BLUEPRINT_SLOT, blueprint);
        }

        SlimefunItem sfi = SlimefunItem.getByItem(blueprint);
        if (sfi != null && sfi.isDisabled()) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.disabled_blueprint"));
            sendFeedback(blockMenu.getLocation(), FeedbackType.DISABLED_BLUEPRINT);
            return false;
        }

        if (!isValidBlueprint(sfi)) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.invalid_blueprint"));
            sendFeedback(blockMenu.getLocation(), FeedbackType.INVALID_BLUEPRINT);
            return false;
        }

        // Get the recipe input
        final ItemStack[] inputs = new ItemStack[RECIPE_SLOTS.length];
        int i = 0;
        for (int recipeSlot : RECIPE_SLOTS) {
            ItemStack stackInSlot = blockMenu.getItemInSlot(recipeSlot);
            if (stackInSlot != null) {
                inputs[i] = ItemStackUtil.getCleanItem(stackInSlot.clone());
            }
            i++;
        }

        ItemStack crafted = null;
        ItemStack[] inp = null;

        ItemStack target = blockMenu.getItemInSlot(ITEM_TARGET_SLOT);
        if (target == null || target.getType() == Material.AIR) target = null;

        for (var e : CraftType.map().entrySet()) {
            boolean found = false;
            for (Map.Entry<ItemStack[], ItemStack> entry : e.getValue()) {
                if (testRecipe(e.getKey(), inputs, entry.getKey())) {
                    crafted = entry.getValue();
                    if (target != null && !StackUtils.itemsMatch(crafted, target)) {
                        continue;
                    }
                    inp = entry.getKey().clone();
                    for (int k = 0; k < inp.length; k++) {
                        if (inp[k] != null) {
                            inp[k] = ItemStackUtil.getCleanItem(inp[k]);
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        if (crafted != null) {
            final SlimefunItem sfi2 = SlimefunItem.getByItem(crafted);
            if (sfi2 != null && sfi2.isDisabled()) {
                player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.disabled_output"));
                sendFeedback(blockMenu.getLocation(), FeedbackType.DISABLED_OUTPUT);
                return false;
            }
        }

        if (crafted == null && canTestVanillaRecipe(inputs)) {
            crafted = Bukkit.craftItem(inputs.clone(), player.getWorld(), player);
            inp = new ItemStack[RECIPE_SLOTS.length];
            for (int k = 0; k < RECIPE_SLOTS.length; k++) {
                if (inputs[k] != null) {
                    inp[k] = StackUtils.getAsQuantity(inputs[k], 1);
                }
            }
        }

        if (inp == null || crafted == null || crafted.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.invalid_recipe"));
            sendFeedback(blockMenu.getLocation(), FeedbackType.INVALID_RECIPE);
            return false;
        }

        final ItemStack blueprintClone = StackUtils.getAsQuantity(blueprint, 1);

        blueprintSetter(blueprintClone, inp, crafted.clone());
        if (BlockMenuUtil.fits(blockMenu, blueprintClone, OUTPUT_SLOT)) {
            ItemStack recover = null;
            if (blueprint.getAmount() == 1) {
                recover = root.getItemStack0(blockMenu.getLocation(), new ItemRequest(blueprint, blueprint.getMaxStackSize()));
            }
            blueprint.setAmount(blueprint.getAmount() - 1);
            if (recover != null) {
                BlockMenuUtil.pushItem(blockMenu, recover, BLANK_BLUEPRINT_SLOT);
            }
            int j = 0;
            for (int recipeSlot : RECIPE_SLOTS) {
                ItemStack slotItem = blockMenu.getItemInSlot(recipeSlot);
                if (slotItem != null) {
                    slotItem.setAmount(slotItem.getAmount() - inp[j].getAmount());
                }
                j++;
            }
            BlockMenuUtil.pushItem(blockMenu, blueprintClone, OUTPUT_SLOT);
            sendFeedback(blockMenu.getLocation(), FeedbackType.SUCCESS);
        } else {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.output_full"));
            sendFeedback(blockMenu.getLocation(), FeedbackType.OUTPUT_FULL);
            return false;
        }

        root.removeRootPower(CHARGE_COST);
        return true;
    }

    public void blueprintSetter(ItemStack itemStack, ItemStack @NotNull [] inputs, ItemStack crafted) {
        craftType().blueprintSetter(itemStack, inputs, crafted);
    }

    public boolean isValidBlueprint(SlimefunItem item) {
        return craftType().isValidBlueprint(item);
    }

    public Set<Map.Entry<ItemStack[], ItemStack>> getRecipeEntries() {
        return craftType().getRecipeEntries();
    }

    public boolean testRecipe(CraftType craftType, ItemStack[] inputs, ItemStack[] recipe) {
        return craftType.testRecipe(inputs, recipe);
    }

    public boolean canTestVanillaRecipe(ItemStack[] inputs) {
        return true;
    }
}
