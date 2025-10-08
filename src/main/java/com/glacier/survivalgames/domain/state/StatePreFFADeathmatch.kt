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
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.util.concurrent.CompletableFuture

class StatePreFFADeathmatch(stateMachine: StateMachine<GameState>,
                            context: GameContext,
                            audienceProvider: AudienceProvider,
                            participantService: ParticipantService,
                            mapService: GameMapService
) : StateBase(stateMachine, GameState.PreDeathmatch, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-deathmatch", 10) }

    init {
        remainTime = defaultTime
    }

    override fun enterAsync(): CompletableFuture<Any> {
        return super.enterAsync().thenApply {
            audienceProvider.all().sendMessage { Chat.component("&4Please allow &8[&e$remainTime&8]&4 seconds for all players to load the map.") }
        }
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            stateMachine.sendEvent(GameState.FFADeathmatch)
            return TaskResponse.continueTask()
        }

        remainTime--

        return TaskResponse.continueTask()
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > ONE_MINUTES) remainTime / ONE_MINUTES else remainTime
            val symbols = if (remainTime > ONE_MINUTES) "minutes" else if (remainTime > 1) "seconds" else "second"
            val message = "&8[&e$time&8] &c$symbols until deathmatch!"

            audienceProvider.all().sendMessage { Chat.component(message) }
        }
    }

    override fun shouldBroadcast(): Boolean = remainTime <= 5

    override fun onJoin(player: Player) {
        val world = Bukkit.getWorld(mapService.decideMap.worldName)
        MCSchedulers.getGlobalScheduler().schedule {
            PaperLib.teleportAsync(player, world.spawnLocation).thenAccept {
                player.spectator()
                context.spectators.add(player.uniqueId)
            }
        }
    }

    override fun onMove(e: PlayerMoveEvent) {
        val position = e.player.gameParticipant?.deathmatchPosition
        if (position == null || position == -1) {
            return
        }

        val spawn = mapService.decideMap.deathmatchSpawns[position]
        LocationUtils.getLocationFromString(spawn)?.let {
            if (e.player.location.blockX != it.blockX
                || e.player.location.blockZ != it.blockZ) {

                it.pitch = e.player.location.pitch
                it.yaw = e.player.location.yaw
                PaperLib.teleportAsync(e.player, it)
            }
        }
    }

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
                PaperLib.teleportAsync(e.player, random.location).thenAccept {
                    audienceProvider.player(e.player).sendMessage { Chat.component("Teleporting ${random.displayName}&r.") }
                }
            }
        }
    }
}