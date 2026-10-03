package com.balugaq.netex.api.gui;

import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import io.github.sefiraat.networks.utils.MatchOption;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「物品匹配选项」子界面：开关清除器匹配网络内物品时忽略哪些属性。
 * <p>
 * 每个位置的选项用 1 个 {@code long} 的位标记持久化在 {@link StorageCacheUtils} 中，读取时经
 * {@link #MATCH_OPTION_MAP} 缓存（机器的 tick 可能在异步线程读取，故用并发容器）。
 *
 * @see MatchOption
 */
public class MatchOptionMenu {
    private static final String KEY_MATCH_OPTION = "match_option";

    public static final Map<Location, MatchOption> MATCH_OPTION_MAP = new ConcurrentHashMap<>();

    private static final int CLOSE_SLOT = 53;
    private static final int @NotNull [] TOGGLE_SLOTS = {13, 22, 31, 40, 49};
    private static final long @NotNull [] IGNORE_FLAGS = {
        MatchOption.IGNORE_DURABILITY,
        MatchOption.IGNORE_ENCHANTMENT,
        MatchOption.IGNORE_ATTRIBUTE,
        MatchOption.IGNORE_LORE,
        MatchOption.IGNORE_CUSTOM_MODEL_DATA
    };
    private static final String @NotNull [] ICON_KEYS = {
        "match-option.ignore-durability",
        "match-option.ignore-enchantment",
        "match-option.ignore-attribute",
        "match-option.ignore-lore",
        "match-option.ignore-custom-model-data"
    };
    private static final Material @NotNull [] MATERIALS = {
        Material.IRON_SWORD,
        Material.ENCHANTED_BOOK,
        Material.DIAMOND_CHESTPLATE,
        Material.WRITTEN_BOOK,
        Material.FILLED_MAP
    };

    private MatchOptionMenu() {}

    /** 读取（并缓存）某个位置的匹配选项，没有存过的内容返回 {@link MatchOption#DEFAULT} */
    public static @NotNull MatchOption getMatchOption(@NotNull Location location) {
        return MATCH_OPTION_MAP.computeIfAbsent(location.clone(), MatchOptionMenu::load);
    }

    /** 写入匹配选项，落盘到 {@link StorageCacheUtils} */
    public static void setMatchOption(@NotNull Location location, @NotNull MatchOption option) {
        MATCH_OPTION_MAP.put(location.clone(), option);
        StorageCacheUtils.setData(location, KEY_MATCH_OPTION, Long.toString(option.getIgnoreFlags()));
    }

    /** 打开「物品匹配选项」界面 */
    @SuppressWarnings("deprecation")
    public static void openMenu(@NotNull Location location, @NotNull Player player) {
        final MatchOption option = getMatchOption(location);
        final ChestMenu menu = new ChestMenu(Lang.getString("messages.match-option.title"));
        menu.setSize(54);

        for (int slot = 0; slot < 54; slot++) {
            menu.addItem(slot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i = 0; i < TOGGLE_SLOTS.length; i++) {
            final int slot = TOGGLE_SLOTS[i];
            final long flag = IGNORE_FLAGS[i];
            final boolean enabled = (option.getIgnoreFlags() & flag) != 0;

            menu.addItem(slot, getToggleStack(ICON_KEYS[i], MATERIALS[i], enabled));
            menu.addMenuClickHandler(slot, (p, s, item, action) -> {
                final MatchOption current = getMatchOption(location);
                setMatchOption(location, current.withIgnore(flag, !enabled));
                openMenu(location, p);
                return false;
            });
        }

        menu.addItem(CLOSE_SLOT, Icon.MATCH_OPTION_CLOSE);
        menu.addMenuClickHandler(CLOSE_SLOT, (p, s, item, action) -> {
            p.closeInventory();
            return false;
        });

        menu.open(player);
    }

    private static @NotNull ItemStack getToggleStack(
        @NotNull String iconKey, @NotNull Material material, boolean enabled) {
        ItemStack stack = Lang.getIcon(iconKey, material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        final List<String> lore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
        lore.add(TextUtil.GRAY + Lang.getString("messages.match-option.click_tip"));
        lore.add(enabled
            ? TextUtil.GREEN + Lang.getString("messages.match-option.enabled")
            : TextUtil.RED + Lang.getString("messages.match-option.disabled"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static @NotNull MatchOption load(@NotNull Location location) {
        final String raw = StorageCacheUtils.getData(location, KEY_MATCH_OPTION);
        if (raw == null) {
            return MatchOption.DEFAULT;
        }
        try {
            return MatchOption.of(Long.parseLong(raw.trim()));
        } catch (NumberFormatException e) {
            return MatchOption.DEFAULT;
        }
    }
}
