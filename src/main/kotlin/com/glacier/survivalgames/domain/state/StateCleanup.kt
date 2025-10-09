package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.extension.spectator
import com.glacier.survivalgames.utils.Chat
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.util.concurrent.CompletableFuture

class StateCleanup(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   audienceProvider: AudienceProvider,
                   participantService: ParticipantService,
                   mapService: GameMapService
) : StateBase(stateMachine, GameState.Cleanup, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.cleanup", 10) }

    init {
        remainTime = defaultTime
    }

    override fun enterAsync(): CompletableFuture<Void> {
        return super.enterAsync().thenAcceptAsync({
            audienceProvider.all().sendMessage { Chat.component("&3The server is cleaning up. You will be returned to the lobby in a moment.") }
        }, CPU_POOL)
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            MCSchedulers.getGlobalScheduler().schedule {
                Bukkit.getOnlinePlayers().forEach { it.kickPlayer("Server restarting") }
                BukkitPlugin.INSTANCE.server.shutdown()
            }
            return TaskResponse.continueTask()
        }

        remainTime--

        return TaskResponse.continueTask()
    }

    override fun broadcast() {}

    override fun shouldBroadcast(): Boolean = false

    override fun onJoin(player: Player) {
        val world = Bukkit.getWorld(mapService.decideMap.worldName)
        MCSchedulers.getGlobalScheduler().schedule {
            PaperLib.teleportAsync(player, world.spawnLocation).thenAcceptAsync({
                player.spectator()
                context.spectators.add(player.uniqueId)
            }, CPU_POOL)
        }
    }

    override fun onChat(e: AsyncPlayerChatEvent) {
        CompletableFuture.runAsync({
            if (context.spectators.contains(e.player.uniqueId)) {
                val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
                audienceProvider.all().sendMessage { Chat.component("&8[&e$points&8]&4SPEC&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
            } else {
                audienceProvider.all().sendMessage { Chat.component("&8[&a${e.player.gameParticipant?.bounties}&8]&c${e.player.gameParticipant?.position}&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
            }
        }, CPU_POOL)
    }

    override fun onMove(e: PlayerMoveEvent) {}

    override fun onItemPickup(e: PlayerPickupItemEvent) {
        if (context.spectators.contains(e.player.uniqueId)) {
            e.isCancelled = true
        }
    }

    override fun onEnterVehicle(e: VehicleEnterEvent) {
        if (context.spectators.contains(e.entered.uniqueId)) {
            e.isCancelled = true
        }
    }

    override fun onFoodChange(e: FoodLevelChangeEvent) {
        if (context.spectators.contains(e.entity.uniqueId)) {
            e.isCancelled = true
        }
    }

    override fun onInteract(e: PlayerInteractEvent) {
        if (context.spectators.contains(e.player.uniqueId)) {
            e.isCancelled = true

            if (e.action == Action.RIGHT_CLICK_AIR
                || e.action == Action.RIGHT_CLICK_BLOCK
                || e.action == Action.LEFT_CLICK_BLOCK) {
                val random = Bukkit.getPlayer(context.players.random())
                PaperLib.teleportAsync(e.player, random.location).thenAcceptAsync({
                    audienceProvider.player(e.player).sendMessage { Chat.component("Teleporting ${random.displayName}&r.") }
                }, CPU_POOL)
            }
        }
    }
}