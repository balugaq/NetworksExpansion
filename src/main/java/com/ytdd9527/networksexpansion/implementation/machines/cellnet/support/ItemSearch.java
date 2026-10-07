package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import com.balugaq.netex.utils.Lang;
import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.Icons;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ChatInput;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.CellnetText;
public final class ItemSearch {

    private ItemSearch() {
    }

    public static boolean matches(@NotNull String search, @Nullable String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            String hay = TextUtil.stripColor(candidate).toLowerCase(Locale.ROOT);
            if (hay.contains(search)) {
                return true;
            }
            if (search.matches("^[a-zA-Z]+$")) {
                if (PinyinHelper.toPinyin(hay, PinyinStyleEnum.INPUT, "").contains(search)) {
                    return true;
                }
                if (PinyinHelper.toPinyin(hay, PinyinStyleEnum.FIRST_LETTER, "").contains(search)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public static <T> List<T> filterByDisplayName(
            @Nullable String search, @NotNull List<T> entries, @NotNull Function<T, String> nameOf) {
        if (search == null) {
            return entries;
        }
        List<T> filtered = new ArrayList<>(entries.size());
        for (T entry : entries) {
            if (matches(search, nameOf.apply(entry))) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static void requestSearch(
            @NotNull Player player,
            @NotNull ChatInput.SearchTarget target,
            @NotNull Location location,
            @Nullable UUID playerUuid,
            @Nullable UUID cellUuid) {
        player.closeInventory();
        boolean replaced = ChatInput.request(player,
            ChatInput.InputType.MONITOR_SEARCH,
            new ChatInput.SearchContext(target, location.clone(), playerUuid, cellUuid));
        if (replaced) {
            player.sendMessage(Lang.getString(CellnetText.INPUT_PREVIOUS_CANCELLED));
        }
        player.sendMessage(Lang.getString(CellnetText.SEARCH_CHAT_HINT));
    }

    @NotNull
    public static ItemStack searchIcon(@Nullable String search) {
        ItemStack icon = Icons.SEARCH_ICON.clone();
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.SEARCH_TITLE));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString(search == null
                ? CellnetText.SEARCH_NONE
                : CellnetText.SEARCH_CURRENT, search));
            lore.add(Lang.getString(CellnetText.SEARCH_HINT));
            meta.setLore(lore);
        });
        return icon;
    }
}
