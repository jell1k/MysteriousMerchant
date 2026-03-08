package org.unixland.mysteriousmerchant.merchant;

import org.bukkit.scheduler.BukkitRunnable;

public class MerchantDespawnTask extends BukkitRunnable {

    private final MerchantManager merchantManager;

    public MerchantDespawnTask(MerchantManager merchantManager) {
        this.merchantManager = merchantManager;
    }

    @Override
    public void run() {
        merchantManager.tick();
    }
}
