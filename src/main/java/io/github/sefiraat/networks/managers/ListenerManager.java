package io.github.sefiraat.networks.managers;

import com.balugaq.netex.core.listeners.HangingBlockInteractListener;
import com.balugaq.netex.core.listeners.JEGCompatibleListener;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell.AECellMenuListener;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive.AEDriveUniquenessListener;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.listeners.ExplosiveToolListener;
import io.github.sefiraat.networks.listeners.SyncListener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ListenerManager {

    public ListenerManager() {
        addListener(new ExplosiveToolListener());
        addListener(new SyncListener());
        if (Networks.getSupportedPluginManager().isJustEnoughGuide()) {
            // todo: remove and deprecate
            try {
                addListener(new JEGCompatibleListener());
            } catch (Throwable ignored) {
                Networks.getSupportedPluginManager().setJustEnoughGuide(false);
            }
        }
        addListener(new HangingBlockInteractListener());
        addListener(new AECellMenuListener());
        addListener(new AEDriveUniquenessListener());
    }

    private void addListener(@NotNull Listener listener) {
        Networks.getPluginManager().registerEvents(listener, Networks.getInstance());
    }
}
