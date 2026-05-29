package com.ytdd9527.networksexpansion.utils.databases;

import com.balugaq.netex.api.data.ItemContainer;
import com.balugaq.netex.api.data.StorageUnitData;
import com.balugaq.netex.api.enums.StorageUnitType;
import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.unit.NetworksDrawer;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DataSource {

    private final String ITEM_ID_KEY = "NEXT_ITEM_ID";
    private final String CONTAINER_ID_KEY = "NEXT_CONTAINER_ID";
    private final @NotNull Logger logger;
    private final @NotNull Map<Integer, ItemStack> itemMap;
    private final @NotNull Map<String, String> environment;
    private Connection conn;
    private int nextContainerId = 0;
    private int nextItemId = 0;

    public DataSource() throws ClassNotFoundException, SQLException {

        connect();
        createTable();
        logger = Networks.getInstance().getLogger();
        // Load item ids
        itemMap = new HashMap<>();
        loadItemMap();
        // Load environment variables
        environment = new HashMap<>();
        loadEnvironment();

        init();
    }

    @SuppressWarnings("deprecation")
    public static @NotNull String getBase64String(ItemStack item) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BukkitObjectOutputStream bs = new BukkitObjectOutputStream(stream);
        bs.writeObject(item);

        bs.close();
        return Base64.getEncoder().encodeToString(stream.toByteArray());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getItemStack(@NotNull String base64Str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64Str));
        BukkitObjectInputStream bs = new BukkitObjectInputStream(stream);
        ItemStack re = (ItemStack) bs.readObject();
        bs.close();
        return re;
    }

    void saveNewStorageData(@NotNull StorageUnitData storageData) {
        String sql = "INSERT INTO " + DataTables.CONTAINER + " VALUES(?, ?, ?, ?, ?)";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, storageData.getId());
                ps.setString(2, storageData.getOwner().getUniqueId().toString());
                ps.setInt(3, storageData.getSizeType().ordinal());
                ps.setInt(4, storageData.isPlaced() ? 1 : 0);
                ps.setString(5, DataStorage.formatLocation(storageData.getLastLocation()));
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-saving-new-data"));
                Debug.trace(e);
                return false;
            }
        });
    }

    int getNextContainerId() {
        int re = nextContainerId++;

        if (!environment.containsKey(CONTAINER_ID_KEY)) {
            String insertSql = "INSERT INTO " + DataTables.ENVIRONMENT + " VALUES (?, ?)";
            environment.put(CONTAINER_ID_KEY, "" + nextContainerId);
            Networks.getQueryQueue().scheduleUpdate(() -> {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, CONTAINER_ID_KEY);
                    ps.setInt(2, nextContainerId);
                    ps.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-environment-var"));
                    Debug.trace(e);
                    return false;
                }
            });
        } else {
            String updateSql = "UPDATE " + DataTables.ENVIRONMENT + " SET VarValue = ? WHERE VarName = ?";
            Networks.getQueryQueue().scheduleUpdate(() -> {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, nextContainerId);
                    ps.setString(2, CONTAINER_ID_KEY);
                    ps.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-environment-var"));
                    Debug.trace(e);
                    return false;
                }
            });
        }

        return re;
    }

    @Nullable
    public StorageUnitData getStorageData(int id) {
        StorageUnitData re = null;

        String sql = "SELECT * FROM " + DataTables.CONTAINER + " WHERE ContainerID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    String[] locStr = result.getString("LastLocation").split(";");
                    Location l = null;

                    if (locStr.length == 4) {
                        World w = Bukkit.getWorld(UUID.fromString(locStr[0]));
                        if (w != null) {
                            l = new Location(
                                w,
                                Integer.parseInt(locStr[1]),
                                Integer.parseInt(locStr[2]),
                                Integer.parseInt(locStr[3]));
                        }
                    }

                    re = new StorageUnitData(
                        result.getInt("ContainerID"),
                        result.getString("PlayerUUID"),
                        StorageUnitType.values()[result.getInt("SizeType") % 13],
                        result.getBoolean("IsPlaced"),
                        l,
                        getStoredItem(id));
                }
            }
        } catch (SQLException e) {
            logger.warning(Lang.getString("messages.data-saving.error-occurred-when-loading-data"));
            Debug.trace(e);
        }
        return re;
    }

    int getItemId(@NotNull ItemStack item) {
        ItemStack clone = item.clone();
        ItemStackWrapper wrapper = ItemStackWrapper.wrap(item);
        for (Map.Entry<Integer, ItemStack> each : itemMap.entrySet()) {
            if (StackUtils.itemsMatch(each.getValue(), wrapper)) {
                return each.getKey();
            }
        }

        // Not found, return new one and
        int re = nextItemId++;

        Networks.getQueryQueue().scheduleUpdate(() -> {
            try {
                // Update environment data
                if (!environment.containsKey(ITEM_ID_KEY)) {
                    String envSql = "INSERT INTO " + DataTables.ENVIRONMENT + " VALUES (?, ?)";
                    environment.put(ITEM_ID_KEY, "" + nextItemId);
                    try (PreparedStatement ps = conn.prepareStatement(envSql)) {
                        ps.setString(1, ITEM_ID_KEY);
                        ps.setInt(2, nextItemId);
                        ps.executeUpdate();
                    }
                } else {
                    String envSql = "UPDATE " + DataTables.ENVIRONMENT + " SET VarValue = ? WHERE VarName = ?";
                    try (PreparedStatement ps = conn.prepareStatement(envSql)) {
                        ps.setInt(1, nextItemId);
                        ps.setString(2, ITEM_ID_KEY);
                        ps.executeUpdate();
                    }
                }

                // Save item map
                String itemSql = "INSERT INTO " + DataTables.ITEM_STACK + " VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    ps.setInt(1, re);
                    ps.setString(2, getBase64String(clone));
                    ps.executeUpdate();
                }
                return true;
            } catch (SQLException | IOException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-saving-itemstack"));
                Debug.trace(e);
                return false;
            }
        });

        return re;
    }

    void updateContainerIsPlaced(int id, int value) {
        String sql = "UPDATE " + DataTables.CONTAINER + " SET IsPlaced = ? WHERE ContainerID = ?";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, value);
                ps.setInt(2, id);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-container-data"));
                Debug.trace(e);
                return false;
            }
        });
    }

    void updateContainerSizeType(int id, int value) {
        String sql = "UPDATE " + DataTables.CONTAINER + " SET SizeType = ? WHERE ContainerID = ?";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, value);
                ps.setInt(2, id);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-container-data"));
                Debug.trace(e);
                return false;
            }
        });
    }

    void updateContainerLastLocation(int id, String value) {
        String sql = "UPDATE " + DataTables.CONTAINER + " SET LastLocation = ? WHERE ContainerID = ?";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, value);
                ps.setInt(2, id);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-container-data"));
                Debug.trace(e);
                return false;
            }
        });
    }

    void addStoredItem(int containerId, int itemId, int amount) {
        if (amount <= 0) return;
        String sql = "INSERT INTO " + DataTables.ITEM_STORED + " VALUES(?, ?, ?)";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, containerId);
                ps.setInt(2, itemId);
                ps.setInt(3, amount);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-storage"));
                Debug.trace(e);
                return false;
            }
        });
    }

    void updateItemAmount(int containerId, int itemId, int amount) {
        if (NetworksDrawer.isLocked(containerId)) {
            if (amount < 0) {
                deleteStoredItem(containerId, itemId);
                return;
            }
        } else if (amount <= 0) {
            deleteStoredItem(containerId, itemId);
            return;
        }
        String sql = "UPDATE " + DataTables.ITEM_STORED + " SET Amount = ? WHERE ContainerID = ? AND ItemID = ?";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setInt(2, containerId);
                ps.setInt(3, itemId);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-storage"));
                Debug.trace(e);
                return false;
            }
        });
    }

    void deleteStoredItem(int containerId, int itemId) {
        String sql = "DELETE FROM " + DataTables.ITEM_STORED + " WHERE ContainerID = ? AND ItemID = ?";
        Networks.getQueryQueue().scheduleUpdate(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, containerId);
                ps.setInt(2, itemId);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.warning(Lang.getString("messages.data-saving.error-occurred-when-updating-storage"));
                Debug.trace(e);
                return false;
            }
        });
    }

    int getIdFromLocation(@NotNull Location l) {
        String sql = "SELECT ContainerID FROM " + DataTables.CONTAINER + " WHERE IsPlaced = 1 AND LastLocation = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DataStorage.formatLocation(l));
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.warning(Lang.getString("messages.data-saving.error-occurred-when-fixing-data"));
            Debug.trace(e);
        }
        return -1;
    }

    private void connect() throws SQLException, ClassNotFoundException {
        File dataFolder = Networks.getInstance().getDataFolder();
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            if (!dataFolder.mkdir()) {
                throw new IllegalStateException(
                    Lang.getString("messages.data-saving.error-occurred-when-creating-data-folder"));
            }
        }
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getPath() + "/CargoStorageUnits.db");
    }

    private void createTable() throws SQLException {
        try (Statement stat = conn.createStatement()) {
            stat.execute(DataTables.CONTAINER_CREATION);
            stat.execute(DataTables.ITEM_STACK_CREATION);
            stat.execute(DataTables.ITEM_STORED_CREATION);
            stat.execute(DataTables.ENVIRONMENT_CREATION);
        }
    }

    private void loadItemMap() {

        String sql = "SELECT Item, ItemID FROM " + DataTables.ITEM_STACK;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                itemMap.put(result.getInt("ItemID"), getItemStack(result.getString("Item")));
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            logger.warning(Lang.getString("messages.data-saving.error-occurred-when-loading-itemstack"));
            Debug.trace(e);
        }
    }

    private void loadEnvironment() {

        String sql = "SELECT VarName, VarValue FROM " + DataTables.ENVIRONMENT;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                environment.put(result.getString(1), result.getString(2));
            }
        } catch (SQLException e) {
            logger.warning(Lang.getString("messages.data-saving.error-occurred-when-loading-environment-var"));
            Debug.trace(e);
        }
    }

    private void init() {
        String temp = environment.get(ITEM_ID_KEY);
        if (temp != null) {
            this.nextItemId = Integer.parseInt(temp);
        }

        temp = environment.get(CONTAINER_ID_KEY);
        if (temp != null) {
            this.nextContainerId = Integer.parseInt(temp);
        }
    }

    @NotNull
    private ConcurrentHashMap<Integer, ItemContainer> getStoredItem(int id) {
        ConcurrentHashMap<Integer, ItemContainer> re = new ConcurrentHashMap<>();

        // Schedule query
        Networks.getQueryQueue().scheduleQuery(new QueuedTask() {
            private boolean success = true;

            @Override
            public boolean execute() {
                String sql =
                    "SELECT ItemID, Amount FROM " + DataTables.ITEM_STORED + " WHERE ContainerID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    try (ResultSet result = ps.executeQuery()) {
                        while (result.next()) {
                            int itemId = result.getInt("ItemID");
                            ItemStack item = itemMap.get(itemId);
                            if (item != null) {
                                re.put(itemId, new ItemContainer(itemId, item, result.getInt("Amount")));
                            }
                        }
                    }
                } catch (SQLException e) {
                    success = false;
                    logger.warning(Lang.getString("messages.data-saving.error-occurred-when-loading-storage"));
                    Debug.trace(e);
                }
                return success;
            }

            @Override
            public boolean callback() {
                // Update container data state
                DataStorage.setContainerLoaded(id);
                return false;
            }
        });

        return re;
    }
}
