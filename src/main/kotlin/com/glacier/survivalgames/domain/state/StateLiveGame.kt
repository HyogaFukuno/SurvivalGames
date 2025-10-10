package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.ChestService
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.entity.getBukkitPlayers
import com.glacier.survivalgames.domain.entity.getMCPlayers
import com.glacier.survivalgames.domain.entity.getMCSpectators
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.extension.spectator
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
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

class StateLiveGame(stateMachine: StateMachine<GameState>,
                    context: GameContext,
                    audienceProvider: AudienceProvider,
                    participantService: ParticipantService,
                    mapService: GameMapService,
                    val chestService: ChestService
) : StateBase(stateMachine, GameState.LiveGame, context, audienceProvider, participantService, mapService) {

    var startedFastDeathmatch = false

    init {
        remainTime = context.settings.gameLength.value
    }

    override fun enterAsync(): CompletableFuture<Void> {
        return super.enterAsync().thenAcceptAsync({
            audienceProvider.all().sendMessage { Chat.component("&3The game have begun!") }
            audienceProvider.all().sendMessage { Chat.component("&aThe deathmatch for the game is &6${context.deathmatchStyle}&a!") }

            val world = Bukkit.getWorld(mapService.decideMap.worldName)
            chestService.tier2Chests.keys.forEach { (x, y, z) ->
                MCSchedulers.getGlobalScheduler().schedule {
                    chestService.fillTier2Chest(chestService.castChest(x, y, z, world))
                }
            }

        }, CPU_POOL)
    }

    override fun update(): TaskResponse<Boolean> {
        if (!startedFastDeathmatch && context.players.size <= 3) {
            remainTime = ONE_MINUTES
            startedFastDeathmatch = true
        }

        if (remainTime % (ONE_MINUTES * 17) == 0) {
            val world = Bukkit.getWorld(mapService.decideMap.worldName)
            MCSchedulers.getGlobalScheduler().schedule {
                chestService.tier2Chests.keys.forEach { (x, y, z) ->
                    chestService.fillTier2Chest(chestService.castChest(x, y, z, world))
                }
            }
            MCSchedulers.getGlobalScheduler().schedule {
                chestService.tier1Chests.keys.forEach { (x, y, z) ->
                    chestService.fillTier1Chest(chestService.getChest(x, y, z, world))
                }
            }

            val tributes = context.getMCPlayers().joinToString { "${it.displayName}&r" }
            audienceProvider.all().sendMessage { Chat.component("&3Sponsors have refilled the chests!") }
            audienceProvider.all().sendMessage { Chat.component("These tributes have passed: $tributes") }
        }

        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            stateMachine.sendEvent(GameState.PreFFADeathmatch)
            return TaskResponse.continueTask()
        }

        if (remainTime % (ONE_MINUTES * 17) == 0) {
            val tributes = context.getMCPlayers().joinToString { "${it.displayName}&r" }
            audienceProvider.all().sendMessage { Chat.component("&3Sponsors have refilled the chests!") }
            audienceProvider.all().sendMessage { Chat.component("These tributes have passed: $tributes") }
        }

        remainTime--

        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Void> {
        if (stateMachine.nextKey == GameState.EndGame) {
            return super.exitAsync()
        }

        val task = MCSchedulers.getGlobalScheduler().schedule {
            val futures = mapService.decideMap.deathmatchSpawns
                .mapNotNull { LocationUtils.getLocationFromString(it) }
                .zip(context.getBukkitPlayers())
                .mapIndexed { index, (location, player) ->
                    PaperLib.teleportAsync(player, location).thenAccept {
                        player.gameParticipant?.deathmatchPosition = index
                    }
                }
            CompletableFuture.allOf(*futures.toTypedArray())
        }

        return task.future
            .thenComposeAsync({ super.exitAsync() }, CPU_POOL)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > ONE_MINUTES) remainTime / ONE_MINUTES else remainTime
            val symbols = if (remainTime > ONE_MINUTES) "minutes" else if (remainTime > 1) "seconds" else "second"
            val message = "&8[&e$time&8] &c$symbols until deathmatch!"

            audienceProvider.all().sendMessage { Chat.component(message) }
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime > ONE_MINUTES && remainTime % (ONE_MINUTES * 5) == 0 -> true

        remainTime <= ONE_MINUTES && remainTime % ONE_MINUTES == 0 -> true
        remainTime <= ONE_MINUTES && remainTime == 30 -> true
        remainTime <= ONE_MINUTES && remainTime == 10 -> true
        remainTime <= ONE_MINUTES && remainTime <= 5 -> true
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