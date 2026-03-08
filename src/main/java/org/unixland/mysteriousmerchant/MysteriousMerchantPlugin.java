package org.unixland.mysteriousmerchant;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.unixland.mysteriousmerchant.command.MMerchantCommand;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.gui.TradeEditorGUI;
import org.unixland.mysteriousmerchant.gui.TradeGUI;
import org.unixland.mysteriousmerchant.listener.PlayerSessionListener;
import org.unixland.mysteriousmerchant.listener.SummonItemListener;
import org.unixland.mysteriousmerchant.merchant.BossBarManager;
import org.unixland.mysteriousmerchant.merchant.HologramManager;
import org.unixland.mysteriousmerchant.merchant.MerchantManager;
import org.unixland.mysteriousmerchant.merchant.MerchantSpawner;
import org.unixland.mysteriousmerchant.trade.TradeManager;

public final class MysteriousMerchantPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private TradeManager tradeManager;
    private MerchantManager merchantManager;
    private MerchantSpawner merchantSpawner;
    private BossBarManager bossBarManager;
    private HologramManager hologramManager;
    private TradeGUI tradeGUI;
    private TradeEditorGUI tradeEditorGUI;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this, configManager);
        this.tradeManager = new TradeManager(this, configManager);
        this.bossBarManager = new BossBarManager(this, configManager, messageManager);
        this.hologramManager = new HologramManager(this, configManager);
        this.merchantManager = new MerchantManager(this, configManager, messageManager, tradeManager, bossBarManager, hologramManager);
        this.merchantSpawner = new MerchantSpawner(this, configManager, merchantManager);
        this.tradeGUI = new TradeGUI(this, tradeManager, merchantManager, messageManager);
        this.tradeEditorGUI = new TradeEditorGUI(this, tradeManager, messageManager);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new SummonItemListener(this, merchantManager, tradeGUI, messageManager), this);
        pm.registerEvents(new PlayerSessionListener(merchantManager, bossBarManager, tradeEditorGUI), this);
        pm.registerEvents(tradeGUI, this);
        pm.registerEvents(tradeEditorGUI, this);

        MMerchantCommand command = new MMerchantCommand(this, merchantManager, tradeEditorGUI, configManager, messageManager);
        if (getCommand("mmerchant") != null) {
            getCommand("mmerchant").setExecutor(command);
            getCommand("mmerchant").setTabCompleter(command);
        }

        merchantSpawner.start();
    }

    @Override
    public void onDisable() {
        if (merchantSpawner != null) {
            merchantSpawner.stop();
        }
        if (merchantManager != null) {
            merchantManager.shutdown();
        }
        if (bossBarManager != null) {
            bossBarManager.shutdown();
        }
        if (hologramManager != null) {
            hologramManager.shutdown();
        }
    }

    public void reloadPlugin() {
        configManager.reloadAll();
        messageManager.reload();
        tradeManager.reload();
        merchantSpawner.restart();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public MerchantManager getMerchantManager() {
        return merchantManager;
    }

    public TradeEditorGUI getTradeEditorGUI() {
        return tradeEditorGUI;
    }
}
