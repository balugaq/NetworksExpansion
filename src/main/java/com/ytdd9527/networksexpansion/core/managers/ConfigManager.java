package com.ytdd9527.networksexpansion.core.managers;

import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.Lang;
import io.github.sefiraat.networks.Networks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ConfigManager {

    public ConfigManager() {
        setupDefaultConfig();
    }

    private void setupDefaultConfig() {
        // config.yml
        final Networks plugin = Networks.getInstance();
        final InputStream inputStream = plugin.getResource("config.yml");
        final File existingFile = new File(plugin.getDataFolder(), "config.yml");

        if (inputStream == null) {
            return;
        }

        final Reader reader = new InputStreamReader(inputStream);
        final FileConfiguration resourceConfig = YamlConfiguration.loadConfiguration(reader);
        final FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(existingFile);

        for (String key : resourceConfig.getKeys(false)) {
            checkKey(existingConfig, resourceConfig, key);
        }

        try {
            existingConfig.save(existingFile);
        } catch (IOException e) {
            Debug.trace(e);
        }
    }

    @ParametersAreNonnullByDefault
    private void checkKey(FileConfiguration existingConfig, FileConfiguration resourceConfig, String key) {
        final Object currentValue = existingConfig.get(key);
        final Object newValue = resourceConfig.get(key);
        if (newValue instanceof ConfigurationSection section) {
            for (String sectionKey : section.getKeys(false)) {
                checkKey(existingConfig, resourceConfig, key + "." + sectionKey);
            }
        } else if (currentValue == null) {
            existingConfig.set(key, newValue);
        }
    }

    public boolean isAutoUpdate() {
        return Networks.getInstance().getConfig().getBoolean("auto-update", false);
    }

    public boolean isDebug() {
        return Networks.getInstance().getConfig().getBoolean("debug", false);
    }

    public @NotNull String getLanguage() {
        return Networks.getInstance().getConfig().getString("language", "zh-CN");
    }

    public boolean isForceCheckLore() {
        return Networks.getInstance().getConfig().getBoolean("rpg-fix.force-check-lore", false);
    }

    public int getPersistentThreshold() {
        return Networks.getInstance().getConfig().getInt("speed-up.persistent-threshold", 15);
    }

    public int getCacheMissThreshold() {
        return Networks.getInstance().getConfig().getInt("speed-up.cache-miss-threshold", 15);
    }

    public int getReduceMs() {
        return Networks.getInstance().getConfig().getInt("speed-down.reduce-ms", 8000);
    }

    public int getTransportMissThreshold() {
        return Networks.getInstance().getConfig().getInt("speed-down.transport-miss-threshold", 120);
    }

    public long getRecordGCThreshold() {
        return Networks.getInstance().getConfig().getLong("record-gc.threshold", 131072);
    }

    public long getRecordGCDeadline() {
        return Networks.getInstance().getConfig().getLong("record-gc.deadline", 120000);
    }

    public boolean isSoftCellBan() {
        return Networks.getInstance().getConfig().getBoolean("speed-up.soft-cell-ban", false);
    }

    public int getSoftCellBanThreshold() {
        return Networks.getInstance().getConfig().getInt("speed-up.soft-cell-ban-threshold", 0);
    }

    public boolean isBanQuantumInQuantum() {
        return Networks.getInstance().getConfig().getBoolean("ban-quantum-in-quantum", false);
    }

    public int getChainBaseDistance() {
        return Networks.getInstance().getConfig().getInt("chain.base-distance", 16);
    }

    public int getChainWorkIntervalSeconds() {
        return Networks.getInstance().getConfig().getInt("chain.work-interval-seconds", 0);
    }

    public boolean isCollectEnabled() {
        return Networks.getInstance().getConfig().getBoolean("collect-rune.enabled", true);
    }

    public int getChainDistancePerModule() {
        return Networks.getInstance().getConfig().getInt("chain.distance-per-module", 16);
    }

    public int getChainMaxDistance() {
        return Networks.getInstance().getConfig().getInt("chain.max-distance", 64);
    }

    public int getChainCacheTtlTicks() {
        return Networks.getInstance().getConfig().getInt("chain.cache-ttl-ticks", 40);
    }

    public int getChainIdleTtlTicks() {
        return Networks.getInstance().getConfig().getInt("chain.idle-ttl-ticks", 200);
    }

    public boolean useBukkitItemComparison() {
        return Networks.getInstance().getConfig().getBoolean("use-bukkit-item-comparison", false);
    }

    public int getInt(@NotNull String path) {
        return getInt(path, 0);
    }

    public int getInt(@NotNull String path, int defaultValue) {
        return Networks.getInstance().getConfig().getInt(path, defaultValue);
    }

    public long getLong(@NotNull String path) {
        return getLong(path, 0);
    }

    public long getLong(@NotNull String path, long defaultValue) {
        return Networks.getInstance().getConfig().getLong(path, defaultValue);
    }

    public void set(@NotNull String path, @NotNull Object value) {
        Networks.getInstance().getConfig().set(path, value);
    }

    public void saveAll() {
        Networks.getInstance().getLogger().info(Lang.getString("messages.save-all"));
    }

    public boolean isDisableProfileCheck() {
        return Networks.getInstance().getConfig().getBoolean("disable-profile-check", false);
    }

    public boolean isFastInteractQuantum() {
        return Networks.getInstance().getConfig().getBoolean("fast-interact-quantum", false);
    }

    public int getCellnetMaxItemTypes() {
        return getInt("cellnet.max-item-types", 64);
    }

    public boolean isAssemblyBatchFetch() {
        return getBoolean("cellnet.assembly-batch-fetch", true);
    }

    public long getCellnetStorageWriteTimeout() {
        return getLong("cellnet-storage.write-timeout-seconds", 10L);
    }

    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return Networks.getInstance().getConfig().getBoolean(path, defaultValue);
    }

    public boolean getCellnetStorageWalMode() {
        return getBoolean("cellnet-storage.wal-mode", true);
    }

    public int getCellnetStorageBusyTimeout() {
        return getInt("cellnet-storage.busy-timeout", 5000);
    }

    public long getCellnetStorageJournalRetentionMinutes() {
        return getLong("cellnet-storage.journal-retention-minutes", 30);
    }

    public boolean getCellnetStorageArchiveEnabled() {
        return getBoolean("cellnet-storage.archive-enabled", true);
    }

    public int getCellnetStorageArchiveRetentionDays() {
        return getInt("cellnet-storage.archive-retention-days", 7);
    }

    public int getCellnetStorageArchiveMaxRows() {
        return getInt("cellnet-storage.archive-max-rows", 2000000);
    }

    public int getCellnetStorageCheckpointInterval() {
        return getInt("cellnet-storage.checkpoint-interval", 60);
    }

    public int getCellnetStorageCheckpointThreshold() {
        return getInt("cellnet-storage.checkpoint-threshold", 5000);
    }

    public boolean getCellnetStorageBackupEnabled() {
        return getBoolean("cellnet-storage.backup-enabled", true);
    }

    public int getCellnetStorageBackupIntervalHours() {
        return getInt("cellnet-storage.backup-interval-hours", 24);
    }

    public int getCellnetStorageBackupKeep() {
        return getInt("cellnet-storage.backup-keep", 10);
    }
}
