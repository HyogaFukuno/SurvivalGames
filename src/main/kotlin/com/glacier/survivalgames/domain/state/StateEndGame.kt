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
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.util.concurrent.CompletableFuture

class StateEndGame(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   audienceProvider: AudienceProvider,
                   participantService: ParticipantService,
                   mapService: GameMapService
) : StateBase(stateMachine, GameState.EndGame, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.end-game", 10) }

    init {
        remainTime = defaultTime
    }

    override fun enterAsync(): CompletableFuture<Void> {
        return super.enterAsync().thenAcceptAsync({
            audienceProvider.all().sendMessage { Chat.component("&aThe games have ended!") }

            // 試合が終了したのでワールドを夜にする
            val world = Bukkit.getWorld(mapService.decideMap.worldName)
            MCSchedulers.getGlobalScheduler().schedule { world.time = 400000L }

            // 全てのスポーン位置に対して花火を出す
            sequenceOf(mapService.decideMap.spawns, mapService.decideMap.deathmatchSpawns)
                .flatten()
                .mapNotNull { LocationUtils.getLocationFromString(it) }
                .forEach { world.spawnEntity(it, EntityType.FIREWORK) }

            context.players.firstOrNull()?.let {
                val player = Bukkit.getPlayer(it)
                player.gameParticipant?.let { gameParticipant ->
                    gameParticipant.wins++
                    if (gameParticipant.bounties > 0) {
                        player.gameParticipant?.points += gameParticipant.bounties
                        audienceProvider.player(player).sendMessage { Chat.component("&aYou've gained &8[&e${gameParticipant.bounties}&8]&a extra points from your bounty!") }
                    }
                }

                audienceProvider.all().sendMessage { Chat.component("${player.displayName}&a has won the Survival Games!") }
            }
        }, CPU_POOL)
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            stateMachine.sendEvent(GameState.Cleanup)
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
        }, IO_POOL)
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