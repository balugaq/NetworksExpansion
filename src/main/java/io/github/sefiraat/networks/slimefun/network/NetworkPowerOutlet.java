package io.github.sefiraat.networks.slimefun.network;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkPowerOutlet extends NetworkDirectional {

    private final int rate;

    public NetworkPowerOutlet(ItemGroup itemGroup,
                              SlimefunItemStack item,
                              RecipeType recipeType,
                              ItemStack[] recipe,
                              int rate
    ) {
        super(itemGroup, item, recipeType, recipe, NodeType.POWER_OUTLET);
        this.rate = rate;
    }

    @Override
    public void onTick(@Nullable BlockMenu menu, @Nonnull Block b) {
        super.onTick(menu, b);
        if (menu == null) {
            return;
        }

        final NodeDefinition definition = NetworkStorage.getNode(b.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        final BlockFace blockFace = getCurrentDirection(menu);
        final Block targetBlock = b.getRelative(blockFace);

        var blockData = StorageCacheUtils.getBlock(targetBlock.getLocation());
        if (blockData == null) {
            return;
        }

        if (!blockData.isDataLoaded()) {
            StorageCacheUtils.requestLoad(blockData);
            return;
        }

        final SlimefunItem slimefunItem = SlimefunItem.getById(blockData.getSfId());
        if (!(slimefunItem instanceof EnergyNetComponent component) || slimefunItem instanceof NetworkObject) {
            return;
        }

        final String charge = blockData.getData("energy-charge");
        int chargeInt = 0;
        if (charge != null) {
            chargeInt = Integer.parseInt(charge);
        }

        final int capacity = component.getCapacity();
        final int space = capacity - chargeInt;

        if (space <= 0) {
            return;
        }

        final int possibleGeneration = Math.min(rate, space);
        final NetworkRoot root = definition.getNode().getRoot();
        final long power = root.getRootPower();

        if (power <= 0) {
            return;
        }

        final int gen = power < possibleGeneration ? (int) power : possibleGeneration;

        component.addCharge(targetBlock.getLocation(), gen);
        root.removeRootPower(gen);
    }
}
