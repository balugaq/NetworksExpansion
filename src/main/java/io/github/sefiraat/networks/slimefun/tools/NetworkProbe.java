package io.github.sefiraat.networks.slimefun.tools;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.ExpansionItemStacks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.NetworksSlimefunItemStacks;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.utils.Theme;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class NetworkProbe extends SpecialSlimefunItem implements CanCooldown {

    public static final String SPACES = " ".repeat(32);
    private static final MessageFormat MESSAGE_FORMAT = new MessageFormat("{0}{1}: {2}{3}", Locale.ROOT);

    public NetworkProbe(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @SuppressWarnings("deprecation")
    private static void displayToPlayer(@NotNull Block block, @NotNull Player player) {
        final NetworkRoot root = NetworkController.getNetworks().get(block.getLocation());
        if (root != null) {
            final int bridges = root.getNodeCount(NodeType.BRIDGE);
            final int monitors = root.getNodeCount(NodeType.STORAGE_MONITOR);
            final int importers = root.getNodeCount(NodeType.IMPORT);
            final int exporters = root.getNodeCount(NodeType.EXPORT);
            final int grids = root.getNodeCount(NodeType.GRID);
            final int cells = root.getNodeCount(NodeType.CELL);
            final int grabbers = root.getNodeCount(NodeType.GRABBER);
            final int pushers = root.getNodeCount(NodeType.PUSHER);
            final int cutters = root.getNodeCount(NodeType.CUTTER);
            final int pasters = root.getNodeCount(NodeType.PASTER);
            final int vacuums = root.getNodeCount(NodeType.VACUUM);
            final int purgers = root.getNodeCount(NodeType.PURGER);
            final int crafters = root.getNodeCount(NodeType.CRAFTER);
            final int powerNodes = root.getNodeCount(NodeType.POWER_NODE);
            final int powerDisplays = root.getNodeCount(NodeType.POWER_DISPLAY);
            final int encoders = root.getNodeCount(NodeType.ENCODER);
            final int wirelessTransmitters = root.getNodeCount(NodeType.WIRELESS_TRANSMITTER);
            final int wirelessReceivers = root.getNodeCount(NodeType.WIRELESS_RECEIVER);
            final int powerOutlets = root.getNodeCount(NodeType.POWER_OUTLET);
            final int greedyBlocks = root.getNodeCount(NodeType.GREEDY_BLOCK);

            final int advancedImporters = root.getNodeCount(NodeType.ADVANCED_IMPORT);
            final int advancedExporters = root.getNodeCount(NodeType.ADVANCED_EXPORT);
            final int advancedGreedyBlocks = root.getNodeCount(NodeType.ADVANCED_GREEDY_BLOCK);
            final int advancedPurgers = root.getNodeCount(NodeType.ADVANCED_PURGER);
            final int advancedVacuums = root.getNodeCount(NodeType.ADVANCED_VACUUM);
            final int transferPushers = root.getNodeCount(NodeType.TRANSFER_PUSHER);
            final int transferGrabbers = root.getNodeCount(NodeType.TRANSFER_GRABBER);
            final int transfers = root.getNodeCount(NodeType.TRANSFER);
            final int lineTransferVanillaPushers =
                root.getNodeCount(NodeType.LINE_TRANSFER_VANILLA_PUSHER);
            final int lineTransferVanillaGrabbers =
                root.getNodeCount(NodeType.LINE_TRANSFER_VANILLA_GRABBER);
            final int inputOnlyMonitor = root.getNodeCount(NodeType.INPUT_ONLY_MONITOR);
            final int outputOnlyMonitor = root.getNodeCount(NodeType.OUTPUT_ONLY_MONITOR);
            final int linePowerOutlets = root.getNodeCount(NodeType.LINE_POWER_OUTLET);
            final int decoders = root.getNodeCount(NodeType.DECODER);
            final int quantumManagers = root.getNodeCount(NodeType.QUANTUM_MANAGER);
            final int drawerManagers = root.getNodeCount(NodeType.DRAWER_MANAGER);
            final int crafterManagers = root.getNodeCount(NodeType.CRAFTER_MANAGER);
            final int itemFlowViewers = root.getNodeCount(NodeType.FLOW_VIEWER);
            final int advancedWirelessTransmitters = root.getNodeCount(NodeType.ADVANCED_WIRELESS_TRANSMITTER);
            final int itemDifferenters = root.getNodeCount(NodeType.ITEM_DIFFERENTER);
            final int storageCardConverters = root.getNodeCount(NodeType.STORAGE_CARD_CONVERTER);
            final int facingPresetters = root.getNodeCount(NodeType.FACING_PRESETTER);
            final int visualGrids = root.getNodeCount(NodeType.VISUAL_GRID);

            final Map<ItemStack, Long> allNetworkItems = root.getAllNetworkItemsLongTypeView();
            final int distinctItems = allNetworkItems.size();

            long totalItems = allNetworkItems.values().stream()
                .mapToLong(integer -> integer)
                .sum();

            final String nodeCount = root.getNodeCount() >= root.getMaxNodes()
                ? Theme.ERROR + String.valueOf(root.getNodeCount()) + "+"
                : String.valueOf(root.getNodeCount());

            final ChatColor c = Theme.CLICK_INFO.getColor();
            final ChatColor p = Theme.SUCCESS.getColor();

            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));
            player.sendMessage(Lang.getString("messages.completed-operation.probe.expansion_title"));
            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));
            player.sendMessage(formatter(ExpansionItemStacks.NETWORK_BLUEPRINT_DECODER.getDisplayName(), decoders));
            player.sendMessage(formatter(ExpansionItemStacks.QUANTUM_MANAGER.getDisplayName(), quantumManagers));
            player.sendMessage(formatter(ExpansionItemStacks.DRAWER_MANAGER.getDisplayName(), drawerManagers));
            player.sendMessage(formatter(ExpansionItemStacks.CRAFTER_MANAGER.getDisplayName(), crafterManagers));
            player.sendMessage(formatter(ExpansionItemStacks.ITEM_FLOW_VIEWER.getDisplayName(), itemFlowViewers));
            player.sendMessage(formatter(ExpansionItemStacks.ADVANCED_WIRELESS_TRANSMITTER.getDisplayName(), advancedWirelessTransmitters));
            player.sendMessage(formatter(ExpansionItemStacks.ITEM_DIFFERENTER.getDisplayName(), itemDifferenters));
            player.sendMessage(formatter(ExpansionItemStacks.STORAGE_CARD_CONVERTER.getDisplayName(), storageCardConverters));
            player.sendMessage(formatter(ExpansionItemStacks.FACING_PRESETTER.getDisplayName(), facingPresetters));
            player.sendMessage(formatter(ExpansionItemStacks.ADVANCED_IMPORT.getDisplayName(), advancedImporters));
            player.sendMessage(formatter(ExpansionItemStacks.ADVANCED_EXPORT.getDisplayName(), advancedExporters));
            player.sendMessage(
                formatter(ExpansionItemStacks.ADVANCED_GREEDY_BLOCK.getDisplayName(), advancedGreedyBlocks));
            player.sendMessage(formatter(ExpansionItemStacks.ADVANCED_PURGER.getDisplayName(), advancedPurgers));
            player.sendMessage(formatter(ExpansionItemStacks.ADVANCED_VACUUM.getDisplayName(), advancedVacuums));
            player.sendMessage(formatter(ExpansionItemStacks.TRANSFER.getDisplayName(), transfers));
            player.sendMessage(formatter(ExpansionItemStacks.TRANSFER_GRABBER.getDisplayName(), transferGrabbers));
            player.sendMessage(formatter(ExpansionItemStacks.TRANSFER_PUSHER.getDisplayName(), transferPushers));
            player.sendMessage(formatter(
                ExpansionItemStacks.LINE_TRANSFER_VANILLA_PUSHER.getDisplayName(), lineTransferVanillaPushers));
            player.sendMessage(formatter(
                ExpansionItemStacks.LINE_TRANSFER_VANILLA_GRABBER.getDisplayName(), lineTransferVanillaGrabbers));
            player.sendMessage(
                formatter(ExpansionItemStacks.NETWORK_INPUT_ONLY_MONITOR.getDisplayName(), inputOnlyMonitor));
            player.sendMessage(
                formatter(ExpansionItemStacks.NETWORK_OUTPUT_ONLY_MONITOR.getDisplayName(), outputOnlyMonitor));
            player.sendMessage(formatter(
                stringOrSpaces(ExpansionItemStacks.LINE_POWER_OUTLET_1.getDisplayName())
                    .substring(0, 6),
                linePowerOutlets));
            player.sendMessage(
                formatter(ExpansionItemStacks.VISUAL_GRID.getDisplayName(), visualGrids));


            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));
            player.sendMessage(Lang.getString("messages.completed-operation.probe.networks_title"));
            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));

            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_BRIDGE.getDisplayName(), bridges));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_MONITOR.getDisplayName(), monitors));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_IMPORT.getDisplayName(), importers));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_EXPORT.getDisplayName(), exporters));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_GRID.getDisplayName(), grids));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_CELL.getDisplayName(), cells));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_GRABBER.getDisplayName(), grabbers));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_PUSHER.getDisplayName(), pushers));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_PURGER.getDisplayName(), purgers));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_AUTO_CRAFTER.getDisplayName(), crafters));
            player.sendMessage(formatter(
                stringOrSpaces(NetworksSlimefunItemStacks.NETWORK_CAPACITOR_1.getDisplayName())
                    .substring(0, 4),
                powerNodes));
            player.sendMessage(
                formatter(NetworksSlimefunItemStacks.NETWORK_POWER_DISPLAY.getDisplayName(), powerDisplays));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_RECIPE_ENCODER.getDisplayName(), encoders));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_CONTROL_X.getDisplayName(), cutters));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_CONTROL_V.getDisplayName(), pasters));
            player.sendMessage(formatter(NetworksSlimefunItemStacks.NETWORK_VACUUM.getDisplayName(), vacuums));
            player.sendMessage(formatter(
                NetworksSlimefunItemStacks.NETWORK_WIRELESS_TRANSMITTER.getDisplayName(), wirelessTransmitters));
            player.sendMessage(formatter(
                NetworksSlimefunItemStacks.NETWORK_WIRELESS_RECEIVER.getDisplayName(), wirelessReceivers));
            player.sendMessage(formatter(
                stringOrSpaces(NetworksSlimefunItemStacks.NETWORK_POWER_OUTLET_1.getDisplayName())
                    .substring(0, 4),
                powerOutlets));
            player.sendMessage(
                formatter(NetworksSlimefunItemStacks.NETWORK_GREEDY_BLOCK.getDisplayName(), greedyBlocks));

            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));
            player.sendMessage(
                formatter(Lang.getString("messages.completed-operation.probe.distinct_items"), distinctItems));
            player.sendMessage(formatter(Lang.getString("messages.completed-operation.probe.total_items"), totalItems));
            player.sendMessage(Lang.getString("messages.completed-operation.probe.split"));
            player.sendMessage(String.format(
                Lang.getString("messages.completed-operation.probe.total_nodes"), nodeCount, root.getMaxNodes()));
            if (root.isOverburdened()) {
                player.sendMessage(Lang.getString("messages.completed-operation.probe.overburdened"));
            }
        }
    }

    public static @NotNull String formatter(String name, long count) {
        return MESSAGE_FORMAT
            .format(
                new Object[]{Theme.CLICK_INFO.getColor(), name, Theme.SUCCESS.getColor(), count},
                new StringBuffer(),
                null)
            .toString();
    }

    public static @NotNull String formatter(String name, String s) {
        return MESSAGE_FORMAT
            .format(
                new Object[]{Theme.CLICK_INFO.getColor(), name, Theme.SUCCESS.getColor(), s},
                new StringBuffer(),
                null)
            .toString();
    }

    public static @NotNull String stringOrSpaces(@Nullable String s) {
        return s == null ? SPACES : s;
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) this::onUse);
    }

    protected void onUse(@NotNull PlayerRightClickEvent e) {
        final Optional<Block> optional = e.getClickedBlock();
        if (optional.isPresent()) {
            final Block block = optional.get();
            final Player player = e.getPlayer();
            if (canBeUsed(player, e.getItem())) {
                SlimefunBlockData blockData = StorageCacheUtils.getBlock(block.getLocation());
                if (blockData == null) {
                    return;
                }

                SlimefunItem slimefunItem = SlimefunItem.getById(blockData.getSfId());
                if (slimefunItem instanceof NetworkController) {
                    e.cancel();
                    displayToPlayer(block, player);
                    putOnCooldown(e.getItem());
                }
            }
        }
    }

    @Override
    public int cooldownDuration() {
        return 10;
    }
}
