package org.unixland.mysteriousmerchant.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.trade.Trade;
import org.unixland.mysteriousmerchant.trade.TradeManager;
import org.unixland.mysteriousmerchant.trade.TradeRarity;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TradeEditorGUI implements Listener {

    private static final String MAIN_TITLE = ColorUtil.colorize("<gradient:#7F00FF:#E100FF>Алтарь Настройки Сделок</gradient>");
    private static final String EDIT_TITLE = ColorUtil.colorize("<gradient:#6A11CB:#2575FC>Ковка Одной Сделки</gradient>");

    private final MysteriousMerchantPlugin plugin;
    private final TradeManager tradeManager;
    private final MessageManager messageManager;

    private final Map<Player, List<Trade>> sessions = new HashMap<>();
    private final Map<Player, Integer> editIndex = new HashMap<>();
    private final Map<Player, TradeRarity> editRarity = new HashMap<>();
    private final Set<UUID> switchingToEditor = new HashSet<>();

    public TradeEditorGUI(MysteriousMerchantPlugin plugin, TradeManager tradeManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.messageManager = messageManager;
    }

    public void openMain(Player player) {
        sessions.put(player, new ArrayList<>(tradeManager.getAllTrades()));
        Inventory inv = Bukkit.createInventory(player, 54, MAIN_TITLE);
        redrawMain(player, inv);
        player.openInventory(inv);
    }

    public void closeSession(Player player) {
        sessions.remove(player);
        editIndex.remove(player);
        editRarity.remove(player);
        switchingToEditor.remove(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (MAIN_TITLE.equals(title)) {
            handleMain(event, player);
            return;
        }
        if (EDIT_TITLE.equals(title)) {
            handleEdit(event, player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (EDIT_TITLE.equals(title)) {
            if (!sessions.containsKey(player)) {
                closeSession(player);
                return;
            }
            saveCurrentEdit(player, event.getInventory());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || !sessions.containsKey(player)) {
                    return;
                }
                Inventory inv = Bukkit.createInventory(player, 54, MAIN_TITLE);
                redrawMain(player, inv);
                player.openInventory(inv);
            });
            return;
        }
        if (MAIN_TITLE.equals(title)) {
            if (switchingToEditor.remove(player.getUniqueId())) {
                return;
            }
            List<Trade> trades = sessions.get(player);
            if (trades != null) {
                tradeManager.saveAll(trades);
                player.sendMessage(messageManager.color("<gradient:#00C853:#B2FF59>Список сделок сохранён в trades.yml</gradient>"));
            }
            closeSession(player);
        }
    }

    private void handleMain(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() >= 54) {
            return;
        }

        List<Trade> trades = sessions.get(player);
        if (trades == null) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 45) {
            Trade newTrade = new Trade("new", TradeRarity.COMMON, new ItemStack(Material.IRON_INGOT, 1), new ItemStack(Material.EMERALD, 1));
            trades.add(newTrade);
            openTradeEditor(player, trades.size() - 1);
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 45 && slot < trades.size()) {
            if (event.getClick() == ClickType.RIGHT) {
                trades.remove(slot);
                redrawMain(player, event.getInventory());
                return;
            }
            openTradeEditor(player, slot);
        }
    }

    private void handleEdit(InventoryClickEvent event, Player player) {
        if (event.getRawSlot() >= 27) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 13 || slot == 22 || slot == 26) {
            event.setCancelled(true);
        }

        if (slot == 22) {
            TradeRarity current = editRarity.getOrDefault(player, TradeRarity.COMMON);
            TradeRarity[] values = TradeRarity.values();
            TradeRarity next = values[(current.ordinal() + 1) % values.length];
            editRarity.put(player, next);
            event.getInventory().setItem(22, rarityButton(next));
            return;
        }

        if (slot == 26) {
            event.setCancelled(true);
            saveCurrentEdit(player, event.getInventory());
            player.closeInventory();
        }
    }

    private void openTradeEditor(Player player, int index) {
        List<Trade> trades = sessions.get(player);
        if (trades == null || index < 0 || index >= trades.size()) {
            return;
        }

        editIndex.put(player, index);
        Trade trade = trades.get(index);
        editRarity.put(player, trade.getRarity());

        Inventory inv = Bukkit.createInventory(player, 27, EDIT_TITLE);
        inv.setItem(10, trade.getCost());
        inv.setItem(13, divider());
        inv.setItem(16, trade.getResult());
        inv.setItem(22, rarityButton(trade.getRarity()));
        inv.setItem(26, saveButton());

        switchingToEditor.add(player.getUniqueId());
        player.openInventory(inv);
    }

    private void saveCurrentEdit(Player player, Inventory inventory) {
        List<Trade> trades = sessions.get(player);
        Integer index = editIndex.get(player);
        if (trades == null || index == null || index < 0 || index >= trades.size()) {
            return;
        }

        ItemStack cost = inventory.getItem(10);
        ItemStack result = inventory.getItem(16);
        if (cost == null || cost.getType().isAir() || result == null || result.getType().isAir()) {
            player.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Пустой обмен сохранить нельзя.</gradient>"));
            return;
        }

        Trade updated = new Trade("trade" + (index + 1), editRarity.getOrDefault(player, TradeRarity.COMMON), cost.clone(), result.clone());
        trades.set(index, updated);
    }

    private void redrawMain(Player player, Inventory inv) {
        inv.clear();
        List<Trade> trades = sessions.get(player);
        if (trades == null) {
            return;
        }

        for (int i = 0; i < Math.min(45, trades.size()); i++) {
            Trade trade = trades.get(i);
            ItemStack icon = trade.getResult();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(ColorUtil.colorize("&#B39DDBЦена: &#FFFFFF" + trade.getCost().getAmount() + "x " + trade.getCost().getType().name()));
            lore.add(ColorUtil.colorize("&#8E54E9Редкость: &#FFFFFF" + trade.getRarity().name()));
            lore.add(ColorUtil.colorize("<gradient:#00C853:#B2FF59>ЛКМ: редактировать</gradient>"));
            lore.add(ColorUtil.colorize("<gradient:#FF512F:#DD2476>ПКМ: удалить</gradient>"));
            if (meta != null) {
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }

        inv.setItem(45, addButton());
        inv.setItem(49, saveExitButton());
        fillEmpty(inv, 46, 54);
    }

    private void fillEmpty(Inventory inv, int from, int to) {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = from; i < to; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, pane);
            }
        }
    }

    private ItemStack addButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#00C853:#B2FF59>Создать новую сделку</gradient>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack saveExitButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#00C853:#B2FF59>Сохранить и выйти</gradient>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack divider() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#AA00FF:#6200EA>✦</gradient>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack rarityButton(TradeRarity rarity) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#8E2DE2:#4A00E0>Редкость: </gradient>&#FFFFFF" + rarity.name()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("<gradient:#FFD200:#FF7A00>Нажмите для смены редкости</gradient>"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack saveButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("<gradient:#00C853:#B2FF59>Подтвердить сделку</gradient>"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
