package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.dao;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection.ConnectionManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal.DirtyTracker;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal.JournalOp;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.template.ItemKeyTplIdBridge;
import io.github.sefiraat.networks.Networks;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class AEStorageCellController {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private final ConnectionManager connMgr;
    private final ItemKeyTplIdBridge bridge;
    private final DirtyTracker dirtyTracker;

    public AEStorageCellController(ConnectionManager connMgr, ItemKeyTplIdBridge bridge, DirtyTracker dirtyTracker) {
        this.connMgr = connMgr;
        this.bridge = bridge;
        this.dirtyTracker = dirtyTracker;
    }

    @NotNull
    public CellData loadData(@NotNull UUID cellUuid) {
        CellData data = new CellData(cellUuid);
        List<CellItemRow> rows = queryCellItems(cellUuid.toString());
        if (!rows.isEmpty()) {
            List<Long> tplIds = new ArrayList<>(rows.size());
            for (CellItemRow row : rows) {
                tplIds.add(row.tplId);
            }
            bridge.preload(tplIds);
        }

        for (CellItemRow row : rows) {
            ItemKey key = bridge.resolveKey(row.tplId);
            if (key == null) {
                LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_resolve_tpl_failed", row.tplId, cellUuid));
                continue;
            }
            // (cell_uuid, tpl_id) 为主键，同一 tpl 不会重复，直接 put 并累加 stored。
            data.storage.put(key.getItemStack(), row.amount);
            data.stored += row.amount;
        }

        loadMeta(cellUuid, data);
        return data;
    }

    private void loadMeta(@NotNull UUID cellUuid, @NotNull CellData data) {
        String sql = "SELECT whitelist_enabled, whitelist FROM ae_cell_meta WHERE cell_uuid = ?";
        try (Connection conn = connMgr.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cellUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.whitelistEnabled = rs.getInt("whitelist_enabled") != 0;
                    data.whitelist = deserializeWhitelist(rs.getBytes("whitelist"));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_load_whitelist_failed", e.getMessage()));
            Debug.trace(e);
        }
    }

    public void markDirty(@NotNull UUID cellUuid, @NotNull ItemKey key, long finalAmount) {
        long tplId = bridge.getOrResolve(key);
        dirtyTracker.record(cellUuid, tplId, finalAmount, finalAmount > 0 ? JournalOp.PUT : JournalOp.REMOVE);
    }

    public void saveWhitelist(@NotNull UUID cellUuid, boolean whitelistEnabled, @NotNull List<ItemStack> whitelist) {
        byte[] whitelistBytes = serializeWhitelist(whitelist);
        long now = System.currentTimeMillis();
        String cellUuidStr = cellUuid.toString();
        // 单条 upsert：行不存在时以 stored=0/snapshot_hash=null 插入；已存在时只更新白名单字段，
        // 避免覆盖 checkpoint 写入的 stored/snapshot_hash。
        String sql = "INSERT INTO ae_cell_meta (cell_uuid, whitelist_enabled, whitelist, stored, snapshot_hash, updated_at, created_at) "
            + "VALUES (?, ?, ?, 0, null, ?, ?) "
            + "ON CONFLICT(cell_uuid) DO UPDATE SET "
            + "whitelist_enabled = excluded.whitelist_enabled, "
            + "whitelist = excluded.whitelist, "
            + "updated_at = excluded.updated_at";
        try (Connection conn = connMgr.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cellUuidStr);
            ps.setInt(2, whitelistEnabled ? 1 : 0);
            ps.setBytes(3, whitelistBytes);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_save_whitelist_failed", e.getMessage()));
            Debug.trace(e);
        }
    }

    private List<CellItemRow> queryCellItems(String cellUuid) {
        List<CellItemRow> rows = new ArrayList<>();
        String sql = "SELECT tpl_id, amount FROM ae_cell_items WHERE cell_uuid = ?";
        try (Connection conn = connMgr.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cellUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CellItemRow(rs.getLong("tpl_id"), rs.getLong("amount")));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_load_items_failed", e.getMessage()));
            Debug.trace(e);
        }
        return rows;
    }

    @NotNull
    private static byte[] serializeWhitelist(@NotNull List<ItemStack> whitelist) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeInt(whitelist.size());
                for (ItemStack item : whitelist) {
                    byte[] bytes = item.serializeAsBytes();
                    out.writeInt(bytes.length);
                    out.write(bytes);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_serialize_whitelist_failed", e.getMessage()));
            Debug.trace(e);
            return new byte[0];
        }
    }

    @NotNull
    private static List<ItemStack> deserializeWhitelist(@Nullable byte[] data) {
        List<ItemStack> whitelist = new ArrayList<>();
        if (data == null || data.length == 0) {
            return whitelist;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int count = in.readInt();
            // 校验条目数，避免恶意/损坏数据导致无界分配
            if (count < 0 || count > data.length / 4) {
                LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_deserialize_whitelist_failed", "invalid entry count " + count));
                return whitelist;
            }
            for (int i = 0; i < count; i++) {
                int length = in.readInt();
                if (length < 0 || length > in.available()) {
                    LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_deserialize_whitelist_failed", "invalid entry length " + length));
                    break;
                }
                byte[] bytes = new byte[length];
                in.readFully(bytes);
                try {
                    ItemStack item = ItemStack.deserializeBytes(bytes);
                    if (item != null && !item.getType().isAir()) {
                        whitelist.add(item);
                    }
                } catch (RuntimeException e) {
                    Debug.trace(e, "白名单条目反序列化失败");
                }
            }
        } catch (IOException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.cell_deserialize_whitelist_failed", e.getMessage()));
            Debug.trace(e);
        }
        return whitelist;
    }

    public static class CellData {
        public final UUID uuid;
        public final java.util.LinkedHashMap<ItemStack, Long> storage = new java.util.LinkedHashMap<>();
        public long stored;
        public boolean whitelistEnabled;
        public List<ItemStack> whitelist = new ArrayList<>();

        public CellData(UUID uuid) {
            this.uuid = uuid;
        }
    }

    private record CellItemRow(long tplId, long amount) {
    }
}
