package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setInitialStatus
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameSettings
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.event.GameForceStartMessage
import com.glacier.survivalgames.domain.model.event.CommandVoteEvent
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.text.DecimalFormat
import java.util.concurrent.CompletableFuture

@InjectableComponent
class StateLobby(stateMachine: StateMachine<GameState>,
                 context: GameContext,
                 participantService: GameParticipantService,
                 mapService: GameMapService
) : StateBase(stateMachine, GameState.Lobby, context, participantService, mapService) {

    companion object {
        val CHANCE_FORMAT = DecimalFormat("0.##")
    }

    private val initialRemainTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.lobby", 10) }
    private val spawnLocation by lazy { LocationUtils.getLocationFromString(BukkitPlugin.INSTANCE.config.getString("settings.lobby-spawn", "")) }
    private val votes = mutableListOf<Player>()

    init {
        remainTime = initialRemainTime
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()
        RxBus.listen<GameForceStartMessage>().subscribe { _ -> stateMachine.sendEvent(GameState.PreGame) }.addTo(disposable)
        RxBus.listen<CommandVoteEvent>().subscribe(this::onVote).addTo(disposable)

        // StateLobbyが実行される前にサーバーに参加しているプレイヤーを追加する
        Bukkit.getOnlinePlayers().forEach {
            it.setInitialStatus()
            context.players.add(it)
        }

        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                if (context.players.size < GameSettings.requiredPlayers) {
                    remainTime = initialRemainTime
                    Bukkit.broadcastMessage(Chat.message("&4Not enough players. restarting timer."))
                }
                else {
                    stateMachine.sendEvent(GameState.PreGame)
                }

            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Any> {
        super.exitAsync()

        val world = Bukkit.getWorld(mapService.playingMap.worldName)
        world.time = 1000L
        val futures = mapService.playingMap.spawns
            .mapNotNull { LocationUtils.getLocationFromString(it) }
            .zip(context.players)
            .mapIndexed { i, (location, player) ->
                PaperLib.teleportAsync(player, location).thenAccept {
                    player.gameParticipant.played++
                    player.gameParticipant.position = i + 1
                    player.gameParticipant.previousMap = mapService.playingMap.name
                }
            }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {  }
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until lobby ends!"))
        }

        if (shouldVoteBroadcast()) {
            voteBroadcast()
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime == 10 -> true
        remainTime <= 5 -> true
        else -> false
    }

    private fun shouldVoteBroadcast(): Boolean = remainTime % 30 == 0

    override fun onPlayerJoin(e: PlayerJoinEvent) {
        super.onPlayerJoin(e)
        spawnLocation?.let { PaperLib.teleportAsync(e.player, it) }
        context.players.add(e.player)
    }

    override fun onPlayerQuit(e: PlayerQuitEvent) {
        super.onPlayerQuit(e)
        context.players.removeIf { it.uniqueId == e.player.uniqueId }
    }

    private fun onVote(e : CommandVoteEvent) {
        if (e.number == null || e.number < 1 || e.number > mapService.votableMaps.size) {
            return
        }

        if (votes.contains(e.player)) {
            e.player.sendMessage(Chat.message("&cYou have already voted"))
            return
        }

        val numVote = when {
            e.player.hasPermission("orca.admin") -> 5
            e.player.hasPermission("orca.moderator") -> 5
            e.player.hasPermission("orca.donor.vip") -> 5
            e.player.hasPermission("orca.donor.quantum") -> 5
            e.player.hasPermission("orca.donor.platinum") -> 4
            e.player.hasPermission("orca.donor.diamond") -> 3
            e.player.hasPermission("orca.donor.gold") -> 2
            else -> 1
        }
        val map = mapService.votableMaps[e.number - 1]
        val oldVote = mapService.currentVotes.getValue(map)
        mapService.currentVotes[map] = oldVote + numVote

        votes.add(e.player)
        e.player.sendMessage(Chat.message("Your map now has &8[&6${oldVote + numVote}&8]&r votes&8."))
    }
}