package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.unixland.mysteriousmerchant.MysteriousMerchantPlugin;

public class SpawnAnimation extends BukkitRunnable {

    private final MysteriousMerchantPlugin plugin;
    private final Location location;
    private final Runnable finishCallback;
    private final int totalTicks;
    private int tick;

    public SpawnAnimation(MysteriousMerchantPlugin plugin, Location location, int seconds, Runnable finishCallback) {
        this.plugin = plugin;
        this.location = location.clone();
        this.finishCallback = finishCallback;
        this.totalTicks = Math.max(60, seconds * 20);
    }

    @Override
    public void run() {
        World world = location.getWorld();
        if (world == null) {
            cancel();
            return;
        }

        if (!plugin.isEnabled()) {
            cancel();
            return;
        }

        double progress = (double) tick / (double) totalTicks;
        double radius = 1.5;
        int points = 18;

        for (int i = 0; i < points; i++) {
            double angle = ((Math.PI * 2) / points) * i + (tick * 0.14);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location p = location.clone().add(x, 0.15 + (Math.sin(tick * 0.15 + i) * 0.07), z);

            if (plugin.getConfig().getBoolean("spawn-animation.particles.portal", true)) {
                world.spawnParticle(Particle.PORTAL, p, 1, 0, 0, 0, 0.03 + (progress * 0.04));
            }
            if (plugin.getConfig().getBoolean("spawn-animation.particles.witch", true)) {
                world.spawnParticle(Particle.SPELL_WITCH, p, 1, 0, 0, 0, 0);
            }
            if (plugin.getConfig().getBoolean("spawn-animation.particles.dragon-breath", true)) {
                world.spawnParticle(Particle.DRAGON_BREATH, p, 1, 0, 0, 0, 0.01 + (progress * 0.03));
            }
        }

        // Magic ritual ring on the ground.
        for (int i = 0; i < 24; i++) {
            double angle = ((Math.PI * 2) / 24.0) * i - (tick * 0.08);
            double x = Math.cos(angle) * 2.0;
            double z = Math.sin(angle) * 2.0;
            world.spawnParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(x, 0.02, z), 1, 0, 0, 0, 0.0);
        }

        if (tick == 20 && plugin.getConfig().getBoolean("spawn-animation.sounds", true)) {
            world.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
            world.playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 0.9f);
        }

        if (tick == 50 && plugin.getConfig().getBoolean("spawn-animation.lightning", true)) {
            world.strikeLightningEffect(location);
        }

        tick += 2;
        if (tick >= totalTicks) {
            cancel();
            finishCallback.run();
        }
    }
}
