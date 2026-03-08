package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Particle;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.scheduler.BukkitRunnable;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;

public class ParticleTask extends BukkitRunnable {

    private final MysteriousMerchantPlugin plugin;
    private final MerchantManager merchantManager;

    public ParticleTask(MysteriousMerchantPlugin plugin, MerchantManager merchantManager) {
        this.plugin = plugin;
        this.merchantManager = merchantManager;
    }

    @Override
    public void run() {
        WanderingTrader trader = merchantManager.getActiveMerchant();
        if (trader == null || trader.isDead()) {
            return;
        }
        if (!plugin.getConfig().getBoolean("effects.particles", true)) {
            return;
        }
        trader.getWorld().spawnParticle(Particle.PORTAL, trader.getLocation().add(0, 1.0, 0), 24, 0.4, 0.6, 0.4, 0.1);
        trader.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, trader.getLocation().add(0, 1.1, 0), 10, 0.4, 0.5, 0.4, 0.03);
        trader.getWorld().spawnParticle(Particle.SPELL_WITCH, trader.getLocation().add(0, 1.0, 0), 8, 0.35, 0.4, 0.35, 0.0);
    }
}
