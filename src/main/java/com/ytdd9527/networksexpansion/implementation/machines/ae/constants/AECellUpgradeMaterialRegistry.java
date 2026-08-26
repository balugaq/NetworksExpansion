package com.ytdd9527.networksexpansion.implementation.machines.ae.constants;

import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * AE 存储元件的升级材料表：元件单格容量 → 对应的量子存储物品.
 */
public final class AECellUpgradeMaterialRegistry {

    private static final Map<AEStorageCellType, NetworkQuantumStorage> UPGRADE_MATERIALS =
        new EnumMap<>(AEStorageCellType.class);

    static {
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_0, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_0);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_1, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_9);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_2, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_10);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_3, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_1);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_4, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_2);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_5, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_3);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_6, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_4);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_7, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_5);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_8, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_6);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_9, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_7);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_10, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_8);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_11, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_11);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_12, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_12);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_13, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_13);
        UPGRADE_MATERIALS.put(AEStorageCellType.LEVEL_14, NetworkSlimefunItems.NETWORK_QUANTUM_STORAGE_14);
    }

    private AECellUpgradeMaterialRegistry() {
    }

    @Nullable
    public static NetworkQuantumStorage getUpgradeMaterial(long maxPerTypeLimit) {
        AEStorageCellType type = AEStorageCellType.fromAmount(maxPerTypeLimit);
        return type == null ? null : UPGRADE_MATERIALS.get(type);
    }

    @Nullable
    public static NetworkQuantumStorage getStorage(@Nullable AEStorageCellType type) {
        return type == null ? null : UPGRADE_MATERIALS.get(type);
    }
}
