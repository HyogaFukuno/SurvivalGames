package com.glacier.survivalgames.presentation.listener

import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockSpreadEvent

@InjectableComponent
@RegisterAsListener
class WorldEventListener : Listener {

    @EventHandler
    fun onBurn(e: BlockBurnEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        e.isCancelled = true
    }

    @EventHandler
    fun onSpread(e: BlockSpreadEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        e.isCancelled = true
    }
}