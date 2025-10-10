package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.entity.getBukkitPlayers
import com.glacier.survivalgames.domain.entity.getMCSpectators
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.extension.spectator
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.util.concurrent.CompletableFuture

class StateFFADeathmatch(stateMachine: StateMachine<GameState>,
                         context: GameContext,
                         audienceProvider: AudienceProvider,
                         participantService: ParticipantService,
                         mapService: GameMapService
) : StateBase(stateMachine, GameState.Deathmatch, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.deathmatch", 10) }
    private var safeArea = 1200
    private var taskAreaLightning: ScheduledTask<*>? = null
    private var taskFinishLightning: ScheduledTask<*>? = null

    init {
        remainTime = defaultTime
    }

    override fun enterAsync(): CompletableFuture<Void> {
        return super.enterAsync().thenAcceptAsync({
            val center = LocationUtils.getLocationFromString(mapService.decideMap.deathmatchCenter)
            taskAreaLightning = MCSchedulers.getAsyncScheduler().scheduleAtFixedRate({
                context.getBukkitPlayers().forEach { player ->
                    val distance = center?.distanceSquared(Location(center.world, player.location.x, center.y, player.location.z))
                    if (distance != null && distance > safeArea) {
                        center.world.strikeLightning(player.location)
                    }
                }
            }, 20L, 20L * 3)

            audienceProvider.all().sendMessage { Chat.component("&cFight to the death!") }
        }, CPU_POOL)
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            stateMachine.sendEvent(GameState.EndGame)
            return TaskResponse.continueTask()
        }

        remainTime--
        safeArea -= 2

        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Void> {
        if (context.players.size <= 1) {
            return super.exitAsync()
        }

        // 範囲用の雷タスクを終了させる
        taskAreaLightning?.cancel()
        taskFinishLightning = MCSchedulers.getAsyncScheduler().scheduleAtFixedRate({
            context.getBukkitPlayers().asSequence().forEach {
                it.world.strikeLightning(it.location)
            }
        }, 0L, 20L * 3)

        return taskFinishLightning!!.future.thenAcceptAsync({
            super.exitAsync()
        }, CPU_POOL)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > ONE_MINUTES) remainTime / ONE_MINUTES else remainTime
            val symbols = if (remainTime > ONE_MINUTES) "minutes" else if (remainTime > 1) "seconds" else "second"
            val message = "&8[&e$time&8] &c$symbols until deathmatch ends!"

            audienceProvider.all().sendMessage { Chat.component(message) }
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime <= 5 -> true
        else -> false
    }

    override fun onJoin(player: Player) {
        val world = Bukkit.getWorld(mapService.decideMap.worldName)
        MCSchedulers.getGlobalScheduler().schedule {
            PaperLib.teleportAsync(player, world.spawnLocation).thenAcceptAsync({
                player.spectator()
                context.spectators.add(player.uniqueId)
            }, CPU_POOL)
        }
    }

    override fun onMove(e: PlayerMoveEvent) {}

    override fun onChat(e: AsyncPlayerChatEvent) {
        CompletableFuture.runAsync({
            // 発言者が観戦者の場合は観戦者とコンソールのみ送信する
            if (context.spectators.contains(e.player.uniqueId)) {
                val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
                context.getMCSpectators().forEach { it.sendMessage { Chat.component("&8[&e$points&8]&4SPEC&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) } }
                audienceProvider.console().sendMessage { Chat.component("&8[&e$points&8]&4SPEC&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
            }
            // 発言者が生存者の場合は全てのユーザー、コンソールに送信する
            else {
                audienceProvider.all().sendMessage { Chat.component("&8[&a${e.player.gameParticipant?.bounties}&8]&c${e.player.gameParticipant?.position}&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
            }
        }, IO_POOL)
    }

    override fun onDamage(e: PlayerDamageEvent) { damage(e) }

    override fun onDamageByPlayer(e: PlayerDamageByPlayerEvent) { damageByPlayer(e) }

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