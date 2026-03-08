package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.List;
import java.util.stream.Collectors;

public final class SummonItemFactory {

    private SummonItemFactory() {
    }

    public static ItemStack create(MysteriousMerchantPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        Material material = Material.matchMaterial(cfg.getString("summon-item.material", "ENDER_EYE"));
        if (material == null) {
            material = Material.ENDER_EYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(cfg.getString("summon-item.name", "<gradient:#7928CA:#FF0080>Призыв Таинственного Торговца</gradient>")));
            List<String> lore = cfg.getStringList("summon-item.lore").stream().map(SummonItemFactory::color).collect(Collectors.toList());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSummonItem(MysteriousMerchantPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemStack configured = create(plugin);
        if (!configured.hasItemMeta()) {
            return false;
        }
        return item.getType() == configured.getType()
                && item.getItemMeta().hasDisplayName()
                && configured.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(configured.getItemMeta().getDisplayName());
    }

    private static String color(String input) {
        return ColorUtil.colorize(input);
    }
}
