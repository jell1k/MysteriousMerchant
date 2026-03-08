package org.unixland.mysteriousmerchant.config;

import org.bukkit.command.CommandSender;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.Map;

public class MessageManager {

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;

    public MessageManager(MysteriousMerchantPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void reload() {
        // ConfigManager reloads files; this class only reads live values.
    }

    public String get(String key) {
        return color(configManager.messages().getString(key, "&cMissing message: " + key));
    }

    public String get(String key, Map<String, String> placeholders) {
        String msg = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public String color(String value) {
        return ColorUtil.colorize(value);
    }
}
