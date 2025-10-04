package com.glacier.survivalgames.presentation.listener

import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntitySpawnEvent

@InjectableComponent
@RegisterAsListener
class WorldEventListener : Listener {

    @EventHandler
    fun onEntitySpawn(e: EntitySpawnEvent) {
        if (e.entityType != EntityType.DROPPED_ITEM) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBurn(e: BlockBurnEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onSpread(e: BlockSpreadEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onFade(e: BlockFadeEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onBreak(e: BlockBreakEvent) {
        if (!breakableBlock(e.block.type)) {
            e.isCancelled = true
        }
    }

    private fun breakableBlock(type: Material): Boolean
            = type == Material.LONG_GRASS ||
            type == Material.LEAVES ||
            type == Material.LEAVES_2 ||
            type == Material.FIRE ||
            type == Material.VINE
}