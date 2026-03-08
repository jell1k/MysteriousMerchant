package org.unixland.mysteriousmerchant.trade;

public enum TradeRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    public static TradeRarity fromString(String value) {
        try {
            return TradeRarity.valueOf(value.toUpperCase());
        } catch (Exception ignored) {
            return COMMON;
        }
    }
}
