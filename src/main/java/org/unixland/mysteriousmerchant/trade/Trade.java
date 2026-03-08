package org.unixland.mysteriousmerchant.trade;

import org.bukkit.inventory.ItemStack;

public class Trade {

    private final String id;
    private final TradeRarity rarity;
    private final ItemStack cost;
    private final ItemStack result;

    public Trade(String id, TradeRarity rarity, ItemStack cost, ItemStack result) {
        this.id = id;
        this.rarity = rarity;
        this.cost = cost;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public TradeRarity getRarity() {
        return rarity;
    }

    public ItemStack getCost() {
        return cost.clone();
    }

    public ItemStack getResult() {
        return result.clone();
    }
}
