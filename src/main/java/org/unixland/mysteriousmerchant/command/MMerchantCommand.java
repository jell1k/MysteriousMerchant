package org.unixland.mysteriousmerchant.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.gui.TradeEditorGUI;
import org.unixland.mysteriousmerchant.merchant.MerchantManager;
import org.unixland.mysteriousmerchant.merchant.SummonItemFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MMerchantCommand implements CommandExecutor, TabCompleter {

    private final MysteriousMerchantPlugin plugin;
    private final MerchantManager merchantManager;
    private final TradeEditorGUI editorGUI;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public MMerchantCommand(MysteriousMerchantPlugin plugin,
                            MerchantManager merchantManager,
                            TradeEditorGUI editorGUI,
                            ConfigManager configManager,
                            MessageManager messageManager) {
        this.plugin = plugin;
        this.merchantManager = merchantManager;
        this.editorGUI = editorGUI;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mmerchant.admin")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(messageManager.color("<gradient:#8E2DE2:#4A00E0>/mmerchant give <player></gradient>"));
            sender.sendMessage(messageManager.color("<gradient:#8E2DE2:#4A00E0>/mmerchant spawn</gradient>"));
            sender.sendMessage(messageManager.color("<gradient:#8E2DE2:#4A00E0>/mmerchant remove</gradient>"));
            sender.sendMessage(messageManager.color("<gradient:#8E2DE2:#4A00E0>/mmerchant editor</gradient>"));
            sender.sendMessage(messageManager.color("<gradient:#8E2DE2:#4A00E0>/mmerchant reload</gradient>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (args.length < 2) {
                    sender.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Использование: /mmerchant give <player></gradient>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Игрок не найден.</gradient>"));
                    return true;
                }
                target.getInventory().addItem(SummonItemFactory.create(plugin));
                sender.sendMessage(messageManager.color("<gradient:#00C853:#B2FF59>Артефакт призыва передан игроку </gradient>&#FFFFFF" + target.getName()));
                return true;
            }
            case "spawn" -> {
                Location loc = plugin.getConfigManager().getSpawnLocations().stream().findAny().orElse(null);
                if (loc == null && sender instanceof Player player) {
                    loc = player.getLocation();
                }
                if (loc == null) {
                    sender.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Нет доступной точки спавна.</gradient>"));
                    return true;
                }
                boolean started = merchantManager.startSpawnSequence(loc, sender instanceof Player p ? p : null, false);
                sender.sendMessage(started
                        ? messageManager.color("<gradient:#00C853:#B2FF59>Ритуал появления торговца запущен.</gradient>")
                        : messageManager.color("<gradient:#FF512F:#DD2476>Торговец уже активен или идёт ритуал.</gradient>"));
                return true;
            }
            case "remove" -> {
                merchantManager.removeMerchant(true);
                sender.sendMessage(messageManager.color("<gradient:#00C853:#B2FF59>Торговец удалён из мира.</gradient>"));
                return true;
            }
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Команда доступна только игрокам.</gradient>"));
                    return true;
                }
                editorGUI.openMain(player);
                return true;
            }
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(messageManager.color("<gradient:#00C853:#B2FF59>MysteriousMerchant перезагружен.</gradient>"));
                return true;
            }
            default -> {
                sender.sendMessage(messageManager.color("<gradient:#FF512F:#DD2476>Неизвестная подкоманда.</gradient>"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mmerchant.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("give", "spawn", "remove", "editor", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String low = input.toLowerCase();
        return values.stream().filter(v -> v.toLowerCase().startsWith(low)).toList();
    }
}
