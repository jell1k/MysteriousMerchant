package org.unixland.mysteriousmerchant.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.gui.TradeGUI;
import org.unixland.mysteriousmerchant.merchant.MerchantManager;
import org.unixland.mysteriousmerchant.merchant.SummonItemFactory;

public class SummonItemListener implements Listener {

    private final MysteriousMerchantPlugin plugin;
    private final MerchantManager merchantManager;
    private final TradeGUI tradeGUI;
    private final MessageManager messageManager;

    public SummonItemListener(MysteriousMerchantPlugin plugin, MerchantManager merchantManager, TradeGUI tradeGUI, MessageManager messageManager) {
        this.plugin = plugin;
        this.merchantManager = merchantManager;
        this.tradeGUI = tradeGUI;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onSummonUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!SummonItemFactory.isSummonItem(plugin, item)) {
            return;
        }

        event.setCancelled(true);
        if (merchantManager.hasActiveMerchant() || merchantManager.isAnimationRunning()) {
            player.sendMessage(messageManager.color("&cТорговец уже присутствует в мире."));
            return;
        }

        consumeOne(player);
        Location location = findSpawnLocation(player.getLocation());
        boolean started = merchantManager.startSpawnSequence(location, player, true);
        if (!started) {
            player.sendMessage(messageManager.color("&cНе удалось призвать торговца."));
            return;
        }

    }

    @EventHandler
    public void onTraderInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!merchantManager.isManagedMerchant(entity.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer().isSneaking()) {
            return;
        }
        tradeGUI.open(event.getPlayer());
    }

    @EventHandler
    public void onTraderDamage(EntityDamageEvent event) {
        if (merchantManager.isManagedMerchant(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTraderTeleport(EntityTeleportEvent event) {
        if (merchantManager.isManagedMerchant(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (SummonItemFactory.isSummonItem(plugin, hand)) {
            hand.setAmount(hand.getAmount() - 1);
            if (hand.getAmount() <= 0) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
            return;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (SummonItemFactory.isSummonItem(plugin, off)) {
            off.setAmount(off.getAmount() - 1);
            if (off.getAmount() <= 0) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }
    }

    private Location findSpawnLocation(Location base) {
        Location loc = base.clone();
        Block block = loc.getBlock();
        while (block.getType().isSolid() && block.getY() < block.getWorld().getMaxHeight() - 2) {
            block = block.getRelative(BlockFace.UP);
        }
        return block.getLocation().add(0.5, 0.0, 0.5);
    }
}
