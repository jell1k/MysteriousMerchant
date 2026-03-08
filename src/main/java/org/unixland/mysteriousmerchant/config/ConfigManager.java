package org.unixland.mysteriousmerchant.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final MysteriousMerchantPlugin plugin;
    private File messagesFile;
    private File tradesFile;
    private FileConfiguration messages;
    private FileConfiguration trades;

    public ConfigManager(MysteriousMerchantPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadMessages();
        loadTrades();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        loadMessages();
        loadTrades();
    }

    public FileConfiguration config() {
        return plugin.getConfig();
    }

    public FileConfiguration messages() {
        return messages;
    }

    public FileConfiguration trades() {
        return trades;
    }

    public void saveTrades() {
        try {
            trades.save(tradesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save trades.yml: " + e.getMessage());
        }
    }

    public List<Location> getSpawnLocations() {
        List<Location> list = new ArrayList<>();
        ConfigurationSection section = config().getConfigurationSection("spawn.locations");
        if (section == null) {
            return list;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection locSec = section.getConfigurationSection(key);
            if (locSec == null) {
                continue;
            }
            World world = Bukkit.getWorld(locSec.getString("world", "world"));
            if (world == null) {
                continue;
            }
            list.add(new Location(world,
                    locSec.getDouble("x"),
                    locSec.getDouble("y"),
                    locSec.getDouble("z")));
        }
        return list;
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadTrades() {
        tradesFile = new File(plugin.getDataFolder(), "trades.yml");
        if (!tradesFile.exists()) {
            plugin.saveResource("trades.yml", false);
        }
        trades = YamlConfiguration.loadConfiguration(tradesFile);
    }
}
