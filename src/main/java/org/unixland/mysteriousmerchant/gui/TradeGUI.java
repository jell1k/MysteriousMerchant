package org.unixland.mysteriousmerchant.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.merchant.MerchantManager;
import org.unixland.mysteriousmerchant.trade.Trade;
import org.unixland.mysteriousmerchant.trade.TradeManager;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TradeGUI implements Listener {

    private static final String TITLE = ColorUtil.colorize("<gradient:#6A11CB:#2575FC>✦ Лавка Тайного Странника ✦</gradient>");

    private final MysteriousMerchantPlugin plugin;
    private final TradeManager tradeManager;
    private final MerchantManager merchantManager;
    private final MessageManager messageManager;

    private final Map<Integer, Trade> rewardSlotMap = new HashMap<>();
    private final Set<Player> transactionLock = new HashSet<>();

    public TradeGUI(MysteriousMerchantPlugin plugin, TradeManager tradeManager, MerchantManager merchantManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.merchantManager = merchantManager;
        this.messageManager = messageManager;
    }

    public void open(Player player) {
        if (!merchantManager.hasActiveMerchant()) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        rewardSlotMap.clear();

        List<Trade> trades = merchantManager.getActiveTrades();
        int max = Math.min(18, trades.size());
        for (int i = 0; i < max; i++) {
            Trade trade = trades.get(i);
            int base = i * 3;
            inv.setItem(base, trade.getCost());
            inv.setItem(base + 1, separator());

            ItemStack reward = trade.getResult();
            ItemMeta meta = reward.getItemMeta();
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(ColorUtil.colorize("&#9B7BFFРедкость: &#E6DBFF" + trade.getRarity().name()));
            lore.add(ColorUtil.colorize("<gradient:#FFD200:#FF7A00>Нажмите, чтобы заключить сделку</gradient>"));
            if (meta != null) {
                meta.setLore(lore);
                reward.setItemMeta(meta);
            }
            int rewardSlot = base + 2;
            rewardSlotMap.put(rewardSlot, trade);
            inv.setItem(rewardSlot, reward);
        }

        for (int i = max * 3; i < 54; i++) {
            inv.setItem(i, emptyPane());
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getRawSlot() >= 54) {
            return;
        }

        Trade trade = rewardSlotMap.get(event.getRawSlot());
        if (trade == null) {
            return;
        }
        if (transactionLock.contains(player)) {
            return;
        }

        transactionLock.add(player);
        try {
            if (!hasEnough(player, trade.getCost())) {
                player.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Недостаточно ресурсов для сделки.</gradient>"));
                return;
            }
            removeItems(player, trade.getCost());
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(trade.getResult());
            leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.sendMessage(messageManager.color("<gradient:#00C853:#B2FF59>Сделка успешно заключена.</gradient>"));
        } finally {
            Bukkit.getScheduler().runTaskLater(plugin, () -> transactionLock.remove(player), 1L);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (TITLE.equals(event.getView().getTitle()) && event.getPlayer() instanceof Player player) {
            transactionLock.remove(player);
        }
    }

    private boolean hasEnough(Player player, ItemStack needed) {
        int required = needed.getAmount();
        int found = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null || content.getType() == Material.AIR) {
                continue;
            }
            if (content.isSimilar(needed)) {
                found += content.getAmount();
                if (found >= required) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItems(Player player, ItemStack needed) {
        int required = needed.getAmount();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null || content.getType() == Material.AIR || !content.isSimilar(needed)) {
                continue;
            }
            int take = Math.min(required, content.getAmount());
            content.setAmount(content.getAmount() - take);
            required -= take;
            if (content.getAmount() <= 0) {
                contents[i] = null;
            }
            if (required <= 0) {
                break;
            }
        }

        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    private ItemStack separator() {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#AA00FF:#6200EA>✦</gradient>"));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack emptyPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
