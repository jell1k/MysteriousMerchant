package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HologramManager {

    private enum Mode {
        ARMORSTAND,
        DECENT_HOLOGRAMS,
        HOLOGRAPHIC_DISPLAYS
    }

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;
    private Mode mode = Mode.ARMORSTAND;

    private final List<ArmorStand> stands = new ArrayList<>();
    private Object externalHologram;
    private String externalId;

    public HologramManager(MysteriousMerchantPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        detectMode();
    }

    public void show(Location base, List<String> lines) {
        remove();
        List<String> colored = lines.stream().map(ColorUtil::colorize).toList();
        if (mode == Mode.DECENT_HOLOGRAMS && createDecent(base, colored)) {
            return;
        }
        if (mode == Mode.HOLOGRAPHIC_DISPLAYS && createHD(base, colored)) {
            return;
        }
        createArmorStand(base, colored);
    }

    public void update(Location base, List<String> lines) {
        show(base, lines);
    }

    public void remove() {
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        stands.clear();

        if (externalHologram != null) {
            try {
                Method delete = externalHologram.getClass().getMethod("delete");
                delete.invoke(externalHologram);
            } catch (Exception ignored) {
            }
            externalHologram = null;
            externalId = null;
        }
    }

    public void shutdown() {
        remove();
    }

    private void createArmorStand(Location base, List<String> lines) {
        double y = base.getY() + 2.8;
        for (String line : lines) {
            ArmorStand stand = (ArmorStand) base.getWorld().spawnEntity(new Location(base.getWorld(), base.getX(), y, base.getZ()), EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stands.add(stand);
            y -= 0.28;
        }
    }

    private boolean createDecent(Location base, List<String> lines) {
        try {
            Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            externalId = "mm_" + System.currentTimeMillis();
            Method create = dhApi.getMethod("createHologram", String.class, Location.class, List.class);
            externalHologram = create.invoke(null, externalId, base.clone().add(0, 2.8, 0), lines);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean createHD(Location base, List<String> lines) {
        try {
            Class<?> apiClass = Class.forName("com.gmail.filoghost.holographicdisplays.api.HologramsAPI");
            Method create = apiClass.getMethod("createHologram", Plugin.class, Location.class);
            externalHologram = create.invoke(null, plugin, base.clone().add(0, 2.8, 0));
            Method append = externalHologram.getClass().getMethod("appendTextLine", String.class);
            for (String line : lines) {
                append.invoke(externalHologram, line);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void detectMode() {
        if (configManager.config().getBoolean("hologram.prefer-decentholograms", true)
                && plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            mode = Mode.DECENT_HOLOGRAMS;
            return;
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            mode = Mode.HOLOGRAPHIC_DISPLAYS;
        }
    }
}
