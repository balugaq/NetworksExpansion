package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.balugaq.netex.utils.Debug;
import io.github.sefiraat.networks.Networks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 驱动器白名单的存取。以 owner 为单位组织并全局共享：
 * 同一 owner 名下的所有驱动器共用一份白名单，持久化于 {@code data/ae_drive_whitelist.yml}.
 */
public final class AEDriveWhitelistManager {

    private static final Map<UUID, Set<UUID>> WHITELIST = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private AEDriveWhitelistManager() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (AEDriveWhitelistManager.class) {
            if (loaded) {
                return;
            }
            load();
            loaded = true;
        }
    }

    @NotNull
    public static List<UUID> getWhitelist(@NotNull UUID ownerUuid) {
        ensureLoaded();
        Set<UUID> set = WHITELIST.get(ownerUuid);
        return set == null ? List.of() : List.copyOf(set);
    }

    public static int getWhitelistSize(@NotNull UUID ownerUuid) {
        ensureLoaded();
        Set<UUID> set = WHITELIST.get(ownerUuid);
        return set == null ? 0 : set.size();
    }

    public static boolean isWhitelisted(@NotNull UUID ownerUuid, @NotNull UUID playerUuid) {
        ensureLoaded();
        Set<UUID> set = WHITELIST.get(ownerUuid);
        return set != null && set.contains(playerUuid);
    }

    public static boolean add(@NotNull UUID ownerUuid, @NotNull UUID playerUuid) {
        ensureLoaded();
        Set<UUID> set = WHITELIST.computeIfAbsent(ownerUuid, k -> ConcurrentHashMap.newKeySet());
        boolean added = set.add(playerUuid);
        if (added) {
            save();
        }
        return added;
    }

    public static boolean remove(@NotNull UUID ownerUuid, @NotNull UUID playerUuid) {
        ensureLoaded();
        Set<UUID> set = WHITELIST.get(ownerUuid);
        if (set == null) {
            return false;
        }
        boolean removed = set.remove(playerUuid);
        if (removed) {
            save();
        }
        return removed;
    }

    private static void load() {
        File file = getFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("whitelist");
        if (section == null) {
            return;
        }
        for (String ownerKey : section.getKeys(false)) {
            UUID ownerUuid;
            try {
                ownerUuid = UUID.fromString(ownerKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Set<UUID> set = WHITELIST.computeIfAbsent(ownerUuid, k -> ConcurrentHashMap.newKeySet());
            for (String uuidStr : section.getStringList(ownerKey)) {
                try {
                    set.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private static void save() {
        File file = getFile();
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : WHITELIST.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                list.add(uuid.toString());
            }
            config.set("whitelist." + entry.getKey(), list);
        }
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Debug.trace(e, "保存 AE 驱动器白名单失败");
        } finally {
            temp.delete();
        }
    }

    @NotNull
    private static File getFile() {
        File dataFolder = new File(Networks.getInstance().getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return new File(dataFolder, "ae_drive_whitelist.yml");
    }
}