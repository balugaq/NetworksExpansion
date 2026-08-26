package com.ytdd9527.networksexpansion.implementation.machines.ae.constants;

import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class AECellUpgradeMaterialRegistry {

    private static final Map<Long, NetworkQuantumStorage> UPGRADE_MATERIALS = new HashMap<>();

    private AECellUpgradeMaterialRegistry() {
    }

    @Nullable
    public static NetworkQuantumStorage getUpgradeMaterial(long maxPerTypeLimit) {
        if (UPGRADE_MATERIALS.isEmpty()) {
            UPGRADE_MATERIALS.put(64L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_0);
            UPGRADE_MATERIALS.put(256L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_9);
            UPGRADE_MATERIALS.put(1024L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_10);
            UPGRADE_MATERIALS.put(4096L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_1);

            UPGRADE_MATERIALS.put(32768L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_2);
            UPGRADE_MATERIALS.put(262144L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_3);
            UPGRADE_MATERIALS.put(2097152L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_4);
            UPGRADE_MATERIALS.put(16777216L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_5);

            UPGRADE_MATERIALS.put(134217728L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_6);
            UPGRADE_MATERIALS.put(1073741824L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_7);
            UPGRADE_MATERIALS.put(2147483647L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_8);
            UPGRADE_MATERIALS.put(34359738352L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_11);

            UPGRADE_MATERIALS.put(549755813888L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_12);
            UPGRADE_MATERIALS.put(8796093022208L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_13);
            UPGRADE_MATERIALS.put(140737488355328L, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_14);
        }
        return UPGRADE_MATERIALS.get(maxPerTypeLimit);
    }
}