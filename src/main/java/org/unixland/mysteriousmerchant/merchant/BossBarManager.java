package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.config.MessageManager;

public class BossBarManager {

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private BossBar bossBar;

    public BossBarManager(MysteriousMerchantPlugin plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    public void show(long secondsLeft, long totalSeconds) {
        if (!configManager.config().getBoolean("bossbar.enabled", true)) {
            return;
        }
        if (bossBar == null) {
            BarColor color = parseColor(configManager.config().getString("bossbar.color", "PURPLE"));
            BarStyle style = parseStyle(configManager.config().getString("bossbar.style", "SOLID"));
            bossBar = Bukkit.createBossBar("", color, style);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        }
        update(secondsLeft, totalSeconds);
        bossBar.setVisible(true);
    }

    public void update(long secondsLeft, long totalSeconds) {
        if (bossBar == null) {
            return;
        }
        double progress = totalSeconds <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, (double) secondsLeft / (double) totalSeconds));
        bossBar.setProgress(progress);
        bossBar.setTitle(messageManager.color("<gradient:#6A11CB:#2575FC>Таинственный торговец покинет мир через </gradient>&#FFFFFF" + format(secondsLeft)));
    }

    public void addPlayer(Player player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public void hide() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    public void shutdown() {
        hide();
    }

    private String format(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private BarColor parseColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseStyle(String name) {
        try {
            return BarStyle.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return BarStyle.SOLID;
        }
    }
}
