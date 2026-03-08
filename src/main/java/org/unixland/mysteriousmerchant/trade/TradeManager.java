package org.unixland.mysteriousmerchant.trade;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class TradeManager {

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private final Map<String, Trade> trades = new LinkedHashMap<>();
    private final EnumMap<TradeRarity, Integer> rarityWeights = new EnumMap<>(TradeRarity.class);

    public TradeManager(MysteriousMerchantPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reload();
    }

    public void reload() {
        trades.clear();
        reloadWeights();
        FileConfiguration cfg = configManager.trades();
        ConfigurationSection section = cfg.getConfigurationSection("trades");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection tradeSec = section.getConfigurationSection(key);
            if (tradeSec == null) {
                continue;
            }
            TradeRarity rarity = TradeRarity.fromString(tradeSec.getString("rarity", "COMMON"));
            ItemStack cost = readItem(tradeSec.getConfigurationSection("cost"));
            ItemStack result = readItem(tradeSec.getConfigurationSection("result"));
            if (cost == null || result == null) {
                plugin.getLogger().warning("Invalid trade in trades.yml: " + key);
                continue;
            }
            trades.put(key, new Trade(key, rarity, cost, result));
        }
    }

    public void saveAll(List<Trade> updatedTrades) {
        FileConfiguration cfg = configManager.trades();
        cfg.set("trades", null);
        int i = 1;
        for (Trade trade : updatedTrades) {
            String base = "trades.trade" + i++;
            cfg.set(base + ".rarity", trade.getRarity().name());
            writeItem(cfg, base + ".cost", trade.getCost());
            writeItem(cfg, base + ".result", trade.getResult());
        }
        configManager.saveTrades();
        reload();
    }

    public List<Trade> getAllTrades() {
        return new ArrayList<>(trades.values());
    }

    public List<Trade> pickRandomTrades(int amount) {
        List<Trade> pool = new ArrayList<>(trades.values());
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        List<Trade> selected = new ArrayList<>();
        while (!pool.isEmpty() && selected.size() < amount) {
            TradeRarity rarity = pickRarityFromPool(pool);
            List<Trade> rarityPool = pool.stream()
                    .filter(trade -> trade.getRarity() == rarity)
                    .collect(Collectors.toList());
            if (rarityPool.isEmpty()) {
                rarityPool = new ArrayList<>(pool);
            }
            Trade chosen = rarityPool.get(random.nextInt(rarityPool.size()));
            selected.add(chosen);
            pool.remove(chosen);
        }
        return selected;
    }

    private TradeRarity pickRarityFromPool(List<Trade> pool) {
        Map<TradeRarity, Integer> effective = new HashMap<>();
        int total = 0;
        for (TradeRarity rarity : TradeRarity.values()) {
            boolean exists = pool.stream().anyMatch(trade -> trade.getRarity() == rarity);
            if (!exists) {
                continue;
            }
            int weight = Math.max(1, rarityWeights.getOrDefault(rarity, 1));
            effective.put(rarity, weight);
            total += weight;
        }

        if (total <= 0) {
            return TradeRarity.COMMON;
        }

        int roll = random.nextInt(total) + 1;
        int cursor = 0;
        for (Map.Entry<TradeRarity, Integer> entry : effective.entrySet()) {
            cursor += entry.getValue();
            if (roll <= cursor) {
                return entry.getKey();
            }
        }
        return TradeRarity.COMMON;
    }

    private void reloadWeights() {
        rarityWeights.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rarity-chances");
        for (TradeRarity rarity : TradeRarity.values()) {
            int weight = section != null ? section.getInt(rarity.name(), 1) : 1;
            rarityWeights.put(rarity, Math.max(0, weight));
        }
    }

    private ItemStack readItem(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            return null;
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, amount);
        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        if ((name != null && !name.isEmpty()) || !lore.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ColorUtil.colorize(name));
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(ColorUtil::colorize).collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void writeItem(FileConfiguration cfg, String path, ItemStack item) {
        cfg.set(path + ".material", item.getType().name());
        cfg.set(path + ".amount", item.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                cfg.set(path + ".name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                cfg.set(path + ".lore", meta.getLore());
            }
        }
    }
}
