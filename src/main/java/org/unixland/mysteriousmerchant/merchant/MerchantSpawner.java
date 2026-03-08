package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;

import java.util.List;
import java.util.Random;

public class MerchantSpawner {

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;
    private final MerchantManager merchantManager;
    private final Random random = new Random();

    private BukkitTask task;

    public MerchantSpawner(MysteriousMerchantPlugin plugin, ConfigManager configManager, MerchantManager merchantManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.merchantManager = merchantManager;
    }

    public void start() {
        if (!configManager.config().getBoolean("spawn.enabled", true)) {
            return;
        }
        long intervalHours = Math.max(1L, configManager.config().getLong("spawn.interval-hours", 4L));
        long intervalTicks = intervalHours * 60L * 60L * 20L;

        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (merchantManager.hasActiveMerchant() || merchantManager.isAnimationRunning()) {
                return;
            }
            Location location = pickLocation();
            if (location != null) {
                merchantManager.startSpawnSequence(location, null, false);
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    public Location pickLocation() {
        List<Location> locations = configManager.getSpawnLocations();
        if (locations.isEmpty()) {
            return null;
        }
        return locations.get(random.nextInt(locations.size())).clone();
    }
}
