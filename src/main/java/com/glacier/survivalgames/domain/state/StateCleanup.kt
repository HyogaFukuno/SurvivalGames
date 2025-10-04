package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setSpectator
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.utils.Chat
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.collections.contains

@InjectableComponent
class StateCleanup(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   participantService: GameParticipantService,
                   mapService: GameMapService
) : StateBase(stateMachine, GameState.Cleanup, context, participantService, mapService) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.cleanup", 10)
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()

        Bukkit.broadcastMessage(Chat.message("&3The server is cleaning up. You will be returned to the lobby in a moment."))
        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {
        when (remainTime) {
            1 -> {
                remainTime = 0
                Bukkit.getOnlinePlayers().forEach { it.player.kickPlayer("Server restarting") }
                BukkitPlugin.INSTANCE.server.shutdown()
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun broadcast() {}

    override fun shouldBroadcast(): Boolean = false

    override fun onPlayerJoin(e: PlayerJoinEvent) {
        super.onPlayerJoin(e)

        val world = Bukkit.getWorld(mapService.playingMap.worldName)
        PaperLib.teleportAsync(e.player, world.spawnLocation).thenAccept {
            e.player.setSpectator()
            context.spectators.add(e.player)
        }
    }

    override fun onPlayerQuit(e: PlayerQuitEvent) {
        super.onPlayerQuit(e)

        context.players.removeIf { it.uniqueId == e.player.uniqueId }
        context.spectators.removeIf { it.uniqueId == e.player.uniqueId }
    }

    override fun onChat(e: AsyncPlayerChatEvent) {
        val participant = e.player.gameParticipant
        val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)

        if (context.spectators.contains(e.player)) {
            e.format = Chat.message("&8[&e$points&8]&4SPEC&8|${e.player.displayName}&8: &r${e.message}", prefix = false)
        } else {
            e.format = Chat.message("&8[&a${participant.bounties}&8]&c${participant.position}&8|${e.player.displayName}&8: &r${e.message}", prefix = false)
        }
    }

    override fun onFoodChange(e: FoodLevelChangeEvent) {
        if (context.spectators.contains(e.entity)) {
            e.isCancelled = true
        }
    }

    override fun onPickupItem(e: PlayerPickupItemEvent) {
        if (context.spectators.contains(e.player)) {
            e.isCancelled = true
        }
    }

    override fun onInteract(e: PlayerInteractEvent) {
        if (context.spectators.contains(e.player)) {
            e.isCancelled = true

            if (e.action == Action.RIGHT_CLICK_AIR
                || e.action == Action.RIGHT_CLICK_BLOCK
                || e.action == Action.LEFT_CLICK_BLOCK) {
                val random = context.players.random()
                PaperLib.teleportAsync(e.player, random.location).thenAccept {
                    e.player.sendMessage(Chat.message("Teleporting ${random.displayName}&r."))
                }
            }
        }
    }

    override fun onEnterVehicle(e: VehicleEnterEvent) {
        if (context.spectators.contains(e.entered)) {
            e.isCancelled = true
        }
    }
}