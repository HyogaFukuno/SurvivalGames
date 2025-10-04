package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setSpectator
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.BountyService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.EntityType
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
class StateEndGame(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   participantService: GameParticipantService,
                   mapService: GameMapService,
                   val bountyService: BountyService
) : StateBase(stateMachine, GameState.EndGame, context, participantService, mapService) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.end-game", 10)
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()

        Bukkit.broadcastMessage(Chat.message("&aThe games have ended!"))

        val world = Bukkit.getWorld(mapService.playingMap.worldName)
        world.time = 400000L
        mapService.playingMap.spawns
            .mapNotNull { LocationUtils.getLocationFromString(it) }
            .forEach { world.spawnEntity(it, EntityType.FIREWORK) }

        mapService.playingMap.deathmatchSpawns
            .mapNotNull { LocationUtils.getLocationFromString(it) }
            .forEach { world.spawnEntity(it, EntityType.FIREWORK) }

        val winner = context.players.firstOrNull()
        winner?.let {
            it.gameParticipant.wins++
            if (it.gameParticipant.bounties > 0) {
                it.sendMessage(Chat.message("&aYou've gained &8[&e${it.gameParticipant.bounties}&8]&a extra points from your bounty!"))
                it.gameParticipant.points += it.gameParticipant.bounties
            }

            Bukkit.broadcastMessage(Chat.message("${it.displayName} &ahas won the Survival Games!"))

            // 優勝者にバウンティーした人がいた場合はその人たちにかけたポイントx1.5返す
            bountyService.getBountiesFromTarget(it).forEach { bounty ->
                val points = (bounty.amount * 1.5).toInt()
                bounty.player.gameParticipant.points += points
                bounty.player.sendMessage(Chat.message("&3You've gained &8[&e$points&8]&3 extra points from your bounty winner ${bounty.target.displayName}&8!"))
                bounty.player.playSound(bounty.player.location, Sound.LEVEL_UP, 1.0f, 1.0f)
            }
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {
        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.Cleanup)
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