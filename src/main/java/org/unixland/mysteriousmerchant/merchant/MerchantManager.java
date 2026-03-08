package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;
import org.unixland.mysteriousmerchant.config.ConfigManager;
import org.unixland.mysteriousmerchant.config.MessageManager;
import org.unixland.mysteriousmerchant.trade.Trade;
import org.unixland.mysteriousmerchant.trade.TradeManager;
import org.unixland.mysteriousmerchant.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MerchantManager {

    private final MysteriousMerchantPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TradeManager tradeManager;
    private final BossBarManager bossBarManager;
    private final HologramManager hologramManager;

    private final Random random = new Random();

    private WanderingTrader activeMerchant;
    private List<Trade> activeTrades = new ArrayList<>();
    private long despawnAtMillis;
    private long totalLifetimeSec;
    private Location anchorLocation;

    private BukkitTask despawnTask;
    private BukkitTask particleTask;
    private BukkitTask animationTask;

    private boolean animationRunning;

    public MerchantManager(MysteriousMerchantPlugin plugin,
                           ConfigManager configManager,
                           MessageManager messageManager,
                           TradeManager tradeManager,
                           BossBarManager bossBarManager,
                           HologramManager hologramManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.tradeManager = tradeManager;
        this.bossBarManager = bossBarManager;
        this.hologramManager = hologramManager;
    }

    public boolean hasActiveMerchant() {
        return activeMerchant != null && !activeMerchant.isDead();
    }

    public WanderingTrader getActiveMerchant() {
        return hasActiveMerchant() ? activeMerchant : null;
    }

    public List<Trade> getActiveTrades() {
        return activeTrades;
    }

    public boolean isManagedMerchant(UUID uuid) {
        return hasActiveMerchant() && activeMerchant.getUniqueId().equals(uuid);
    }

    public boolean isAnimationRunning() {
        return animationRunning;
    }

    public Location getAnchorLocation() {
        return anchorLocation == null ? null : anchorLocation.clone();
    }

    public boolean startSpawnSequence(Location location, Player summoner, boolean announceSummon) {
        if (hasActiveMerchant() || animationRunning) {
            return false;
        }

        Location spawnLoc = location.clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);
        this.anchorLocation = spawnLoc.clone();

        boolean animationEnabled = plugin.getConfig().getBoolean("spawn-animation.enabled", true);
        if (animationEnabled) {
            animationRunning = true;
            int duration = plugin.getConfig().getInt("spawn-animation.duration-seconds", 4);
            SpawnAnimation animation = new SpawnAnimation(plugin, spawnLoc, duration, () -> {
                animationRunning = false;
                spawnMerchant(spawnLoc, summoner, announceSummon);
            });
            animationTask = animation.runTaskTimer(plugin, 0L, 2L);
            return true;
        }

        spawnMerchant(spawnLoc, summoner, announceSummon);
        return true;
    }

    public void removeMerchant(boolean announceDespawn) {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        animationRunning = false;

        if (activeMerchant != null && !activeMerchant.isDead()) {
            activeMerchant.remove();
        }
        activeMerchant = null;
        activeTrades = new ArrayList<>();
        anchorLocation = null;

        if (despawnTask != null) {
            despawnTask.cancel();
            despawnTask = null;
        }
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        bossBarManager.hide();
        hologramManager.remove();

        if (announceDespawn) {
            Bukkit.broadcastMessage(messageManager.get("despawn"));
        }
    }

    public void tick() {
        if (!hasActiveMerchant()) {
            removeMerchant(false);
            return;
        }

        activeMerchant.teleport(anchorLocation);

        long left = Math.max(0L, (despawnAtMillis - System.currentTimeMillis()) / 1000L);
        if (left <= 0) {
            removeMerchant(true);
            return;
        }

        bossBarManager.show(left, totalLifetimeSec);
        bossBarManager.update(left, totalLifetimeSec);

        List<String> lines = Arrays.asList(
                "<gradient:#6A11CB:#2575FC>✦ Таинственный торговец ✦</gradient>",
                "<gradient:#FF9966:#FF5E62>Редкости со всех уголков миров</gradient>",
                "&#D0D0D0Исчезнет через: &#FFFFFF" + format(left)
        );
        hologramManager.update(anchorLocation, lines);
    }

    public void shutdown() {
        removeMerchant(false);
    }

    private void spawnMerchant(Location location, Player summoner, boolean announceSummon) {
        activeTrades = tradeManager.pickRandomTrades(10);
        if (activeTrades.isEmpty()) {
            plugin.getLogger().warning("No trades found in trades.yml. Merchant was not spawned.");
            return;
        }

        activeMerchant = location.getWorld().spawn(location, WanderingTrader.class, trader -> {
            trader.setAI(false);
            trader.setInvulnerable(true);
            trader.setCollidable(false);
            trader.setPersistent(true);
            trader.setRemoveWhenFarAway(false);
            trader.setCanDrinkPotion(false);
            trader.setCanDrinkMilk(false);
            trader.setDespawnDelay(-1);
            trader.setCustomNameVisible(true);
            trader.setCustomName(ColorUtil.colorize(randomMerchantName()));
            applyRandomSkin(trader);

            List<MerchantRecipe> recipes = new ArrayList<>();
            for (Trade trade : activeTrades) {
                MerchantRecipe recipe = new MerchantRecipe(trade.getResult(), Integer.MAX_VALUE);
                recipe.addIngredient(trade.getCost());
                recipes.add(recipe);
            }
            trader.setRecipes(recipes);
        });

        anchorLocation = location.clone();

        long lifetimeMinutes = plugin.getConfig().getLong("lifetime-minutes", 5L);
        totalLifetimeSec = lifetimeMinutes * 60L;
        despawnAtMillis = System.currentTimeMillis() + (totalLifetimeSec * 1000L);

        if (despawnTask != null) {
            despawnTask.cancel();
        }
        despawnTask = new MerchantDespawnTask(this).runTaskTimer(plugin, 20L, 20L);

        if (particleTask != null) {
            particleTask.cancel();
        }
        particleTask = new ParticleTask(plugin, this).runTaskTimer(plugin, 0L, 10L);

        applySpawnVisuals();
        sendSpawnMessages(summoner, announceSummon);
    }

    private void applySpawnVisuals() {
        if (activeMerchant == null) {
            return;
        }
        activeMerchant.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 1, false, false, false));

        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            activeMerchant.getWorld().spawnParticle(Particle.TOTEM, activeMerchant.getLocation().add(0, 1, 0), 35, 0.5, 0.7, 0.5, 0.2);
            activeMerchant.getWorld().spawnParticle(Particle.PORTAL, activeMerchant.getLocation().add(0, 1, 0), 45, 0.6, 0.8, 0.6, 0.3);
            activeMerchant.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, activeMerchant.getLocation().add(0, 1.1, 0), 25, 0.5, 0.7, 0.5, 0.1);
            activeMerchant.getWorld().spawnParticle(Particle.SPELL_WITCH, activeMerchant.getLocation().add(0, 1.0, 0), 20, 0.5, 0.6, 0.5, 0.0);
        }
        if (plugin.getConfig().getBoolean("effects.sound", true)) {
            activeMerchant.getWorld().playSound(activeMerchant.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    private void sendSpawnMessages(Player summoner, boolean announceSummon) {
        if (anchorLocation == null) {
            return;
        }

        Map<String, String> placeholders = Map.of(
                "x", String.valueOf(anchorLocation.getBlockX()),
                "y", String.valueOf(anchorLocation.getBlockY()),
                "z", String.valueOf(anchorLocation.getBlockZ())
        );

        Bukkit.broadcastMessage(messageManager.get("spawned", placeholders));
        if (announceSummon && summoner != null) {
            Bukkit.broadcastMessage(messageManager.get("summoned", Map.of("player", summoner.getName())));
        }
    }

    private String randomMerchantName() {
        List<String> names = plugin.getConfig().getStringList("merchant-names");
        if (names.isEmpty()) {
            names = Arrays.asList(
                    "<gradient:#8E2DE2:#4A00E0>Странник из Бездны</gradient>",
                    "<gradient:#FC466B:#3F5EFB>Торговец Теней</gradient>",
                    "<gradient:#00B09B:#96C93D>Путник Миров</gradient>"
            );
        }
        return names.get(random.nextInt(names.size()));
    }

    private void applyRandomSkin(WanderingTrader trader) {
        List<String> variants = plugin.getConfig().getStringList("skin-variants");
        String selected = variants.isEmpty() ? "#6b2d8f" : variants.get(random.nextInt(variants.size()));
        Color color = parseColor(selected);

        ItemStack helmet = leatherPiece(org.bukkit.Material.LEATHER_HELMET, color);
        ItemStack chest = leatherPiece(org.bukkit.Material.LEATHER_CHESTPLATE, color);
        ItemStack legs = leatherPiece(org.bukkit.Material.LEATHER_LEGGINGS, color);
        ItemStack boots = leatherPiece(org.bukkit.Material.LEATHER_BOOTS, color);

        EntityEquipment equipment = trader.getEquipment();
        if (equipment == null) {
            return;
        }
        equipment.setHelmet(helmet);
        equipment.setChestplate(chest);
        equipment.setLeggings(legs);
        equipment.setBoots(boots);
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
    }

    private ItemStack leatherPiece(org.bukkit.Material material, Color color) {
        ItemStack piece = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            piece.setItemMeta(meta);
        }
        return piece;
    }

    private Color parseColor(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() != 6) {
            return Color.fromRGB(107, 45, 143);
        }
        try {
            int rgb = Integer.parseInt(value, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception ignored) {
            return Color.fromRGB(107, 45, 143);
        }
    }

    private String format(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public ItemStack createSummonItem() {
        return SummonItemFactory.create(plugin);
    }
}
