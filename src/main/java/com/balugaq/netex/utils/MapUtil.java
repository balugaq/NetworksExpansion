package com.balugaq.netex.utils;

import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
@NullMarked
public class MapUtil {
    public static final Map<String, MapView> MAP_VIEWS = new HashMap<>();

    public static Pair<ItemStack, MapView> getImageItem(String imagePath) {
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapView view = apply(map, imagePath);
        return new Pair<>(map, view);
    }

    public static @Nullable MapView apply(ItemStack map, String imagePath) {
        if (map.getItemMeta() instanceof MapMeta meta) {
            if (MAP_VIEWS.containsKey(imagePath)) {
                MapView view = MAP_VIEWS.get(imagePath);
                meta.setMapView(view);
                map.setItemMeta(meta);
                return view;
            }

            MapView view;
            if (!meta.hasMapView()) {
                view = Bukkit.createMap(Bukkit.getWorlds().getFirst());
            } else {
                view = meta.getMapView();
                if (view == null) {
                    view = Bukkit.createMap(Bukkit.getWorlds().getFirst());
                }
            }

            BufferedImage finalImage = resizeImage(ImageUtil.getImage(imagePath), 128, 128);
            view.addRenderer(new MapRenderer() {
                @Override
                public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                    mapCanvas.drawImage(0, 0, finalImage);
                }
            });
            view.setScale(MapView.Scale.FARTHEST);
            view.setLocked(true);
            meta.setMapView(view);
            MAP_VIEWS.put(imagePath, view);
            meta.setScaling(true);
            meta.setColor(org.bukkit.Color.GREEN);
            map.setItemMeta(meta);
            return view;
        }

        return null;
    }

    public static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }
}
