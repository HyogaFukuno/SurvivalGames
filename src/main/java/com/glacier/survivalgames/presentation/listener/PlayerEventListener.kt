package com.glacier.survivalgames.presentation.listener

import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent

@InjectableComponent
@RegisterAsListener
class PlayerEventListener : Listener {
    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        e.player.playerListName
        e.joinMessage = null
        RxBus.publish(e)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage = null
        RxBus.publish(e)
    }

    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) = RxBus.publish(e)

    @EventHandler
    fun onDamageByPlayer(e: PlayerDamageByPlayerEvent) = RxBus.publish(e)

    @EventHandler
    fun onDamage(e: PlayerDamageEvent) = RxBus.publish(e)

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) { e.deathMessage = null }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) = RxBus.publish(e)

    @EventHandler
    fun onItemPickup(e: PlayerPickupItemEvent) = RxBus.publish(e)

    @EventHandler
    fun onEnterVehicle(e: VehicleEnterEvent) = RxBus.publish(e)

    @EventHandler
    fun onFoodChange(e: FoodLevelChangeEvent) = RxBus.publish(e)

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) = RxBus.publish(e)

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (e.block.type != Material.FIRE) {
            e.isCancelled = true
            return
        }

        val max = Material.FLINT_AND_STEEL.maxDurability
        val item = e.player.inventory.itemInHand
        item.durability = item.durability.plus(max.div(4)).toShort()

        if (max <= item.durability) {
            e.player.inventory.remove(item)
            e.player.playSound(e.player.location, Sound.ITEM_BREAK, 1.0f, 1.0f)
        }
    }

    @EventHandler
    fun onBucketEmpty(e: PlayerBucketEmptyEvent) { e.isCancelled = true }
}