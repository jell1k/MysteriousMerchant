package org.unixland.mysteriousmerchant.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.unixland.mysteriousmerchant.gui.TradeEditorGUI;
import org.unixland.mysteriousmerchant.merchant.BossBarManager;
import org.unixland.mysteriousmerchant.merchant.MerchantManager;

public class PlayerSessionListener implements Listener {

    private final MerchantManager merchantManager;
    private final BossBarManager bossBarManager;
    private final TradeEditorGUI editorGUI;

    public PlayerSessionListener(MerchantManager merchantManager, BossBarManager bossBarManager, TradeEditorGUI editorGUI) {
        this.merchantManager = merchantManager;
        this.bossBarManager = bossBarManager;
        this.editorGUI = editorGUI;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (merchantManager.hasActiveMerchant()) {
            bossBarManager.addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        editorGUI.closeSession(player);
    }
}
