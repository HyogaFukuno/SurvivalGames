package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.getBukkitPlayers
import com.glacier.survivalgames.domain.message.OpenServerManagementMessage
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import com.glacier.survivalgames.utils.RxBus
import com.glacier.survivalgames.utils.TextColor
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

class StateLobby(stateMachine: StateMachine<GameState>,
                 context: GameContext,
                 audienceProvider: AudienceProvider,
                 participantService: ParticipantService,
                 mapService: GameMapService
) : StateBase(stateMachine, GameState.Lobby, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.lobby", 10) }
    private val lobbySpawn by lazy { BukkitPlugin.INSTANCE.config.getString("settings.lobby-spawn", "") }
    private val compass by lazy {
        val item = ItemStack(Material.COMPASS)
        item.itemMeta = item.itemMeta.apply {
            displayName = TextColor.text("&bServer Management")
        }
        return@lazy item
    }
    private val permissionManagement = Permission("sg.management")


    init {
        remainTime = defaultTime
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0
            if (context.players.size < context.settings.requiredPlayers) {
                remainTime = defaultTime
                audienceProvider.all().sendMessage { Chat.component("&4Not enough players. restarting timer.") }
            }
            else stateMachine.sendEvent(GameState.PreGame)
            return TaskResponse.continueTask()
        }

        remainTime--

        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Void> {
        val task: ScheduledTask<World> = MCSchedulers.getGlobalScheduler().schedule(Callable {
            createWorld(mapService.decideMap.worldName)
        })

        return task.future.thenComposeAsync({
            super.exitAsync().thenCompose {
                val futures = mapService.decideMap.spawns
                    .mapNotNull { LocationUtils.getLocationFromString(it) }
                    .mapIndexed { index, location -> Pair(index, location) }
                    .shuffled()
                    .zip(context.getBukkitPlayers())
                    .map { (pair, player) ->
                        val (index, location) = pair
                        PaperLib.teleportAsync(player, location).thenAccept {
                            player.gameParticipant?.position = index
                        }
                    }
                CompletableFuture.allOf(*futures.toTypedArray())
            }
        }, CPU_POOL)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > ONE_MINUTES) remainTime / ONE_MINUTES else remainTime
            val symbols = if (remainTime > ONE_MINUTES) "minutes" else if (remainTime > 1) "seconds" else "second"
            val message = "&8[&e$time&8] &c$symbols until lobby ends!"

            audienceProvider.all().sendMessage { Chat.component(message) }
        }

        if (shouldBroadcastVote()) {
            broadcastVoteMessage()
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime == 10 -> true
        remainTime <= 5 -> true
        else -> false
    }

    override fun onJoin(player: Player) {
        context.players.add(player.uniqueId)
        if (player.hasPermission(permissionManagement)) {
            player.inventory.setItem(4, compass)
        }

        LocationUtils.getLocationFromString(lobbySpawn)?.let {
            MCSchedulers.getGlobalScheduler().schedule {
                PaperLib.teleportAsync(player, it)
            }
        }
    }

    override fun onMove(e: PlayerMoveEvent) {}

    override fun onInteract(e: PlayerInteractEvent) {
        if (e.item == compass && e.player.hasPermission(permissionManagement)) {
            RxBus.publish(OpenServerManagementMessage(e.player))
        }
    }

    private fun shouldBroadcastVote(): Boolean = remainTime % 30 == 0

    private fun createWorld(name: String): World {
        val world = WorldCreator(name).createWorld().apply {
            isAutoSave = false
            keepSpawnInMemory = false
            time = 1000L
            isThundering = false
        }

        return world
    }
}