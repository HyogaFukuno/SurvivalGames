package com.glacier.survivalgames.presentation.listener

import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.infrastructure.manager.ViewManager
import com.glacier.survivalgames.infrastructure.view.GameMasterMainView
import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission

@InjectableComponent
@RegisterAsListener
class GameMasterListener(val context: GameContext, val viewManager: ViewManager) : Listener {

    companion object {
        private val GAME_MASTER_PERMISSION = Permission("sg.gamemaster")

        private val GAME_MASTER_COMPASS by lazy { getCompass() }

        private fun getCompass(): ItemStack {
            val item = ItemStack(Material.COMPASS)
            val meta = item.itemMeta
            meta.displayName = "Game Master"
            item.itemMeta = meta
            return item
        }
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (context.state != GameState.Lobby || !e.player.hasPermission(GAME_MASTER_PERMISSION)) {
            return
        }
        e.player.inventory.setItem(0, GAME_MASTER_COMPASS)
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (context.state != GameState.Lobby) {
            return
        }

        if (e.action != Action.RIGHT_CLICK_AIR && e.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (e.player.inventory.itemInHand?.type != Material.COMPASS) {
            return
        }

        viewManager.open<GameMasterMainView>(e.player)
    }
}