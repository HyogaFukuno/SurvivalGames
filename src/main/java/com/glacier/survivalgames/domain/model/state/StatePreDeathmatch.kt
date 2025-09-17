package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameParticipant
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.spawnLocation
import com.glacier.survivalgames.domain.model.uniqueId
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class StatePreDeathmatch(stateMachine: StateMachine<GameState>, val participantService: GameParticipantService, val mapService: GameMapService, val tournamentService: TournamentService) : StateMachine.State<GameState>(stateMachine, GameState.PreDeathmatch) {

    val map by lazy { mapService.getPlayingMap() }

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-deathmatch")
    }

    override fun enterAsync(): CompletableFuture<*> {
        Log.info("Entering $key state")
        GameContext.state = key

        when (GameContext.deathmatchType) {
            DeathmatchType.FFA -> Bukkit.broadcastMessage(Chat.message("&4Please allow &8[&e$remainTime&8] &4seconds for all players to load the map."))
            DeathmatchType.Tournament -> {
                val duelist1 = tournamentService.getTournamentDuelist()
                val duelist2 = tournamentService.getTournamentDuelist(duelist1)
                tournamentService.setDuelist(duelist1)
                tournamentService.setDuelist(duelist2)

                Bukkit.broadcastMessage(Chat.message("&4Tournament has launched! ${duelist1.player.displayName} &8vs ${duelist2.player.displayName}&8."))

                // デュエリスト達を指定の位置へテレポートさせる
                map.deathmatchSpawns.mapNotNull { LocationUtils.getLocationFromString(it) }
                    .forEachIndexed { i, location ->
                        if (i == 0) {
                            PaperLib.teleportAsync(duelist1.player, location).thenAccept {
                                participantService.freezers().putIfAbsent(duelist1.uniqueId, location)
                            }
                        } else {
                            PaperLib.teleportAsync(duelist2.player, location).thenAccept {
                                participantService.freezers().putIfAbsent(duelist2.uniqueId, location)
                            }
                        }
                    }

                val others = tournamentService.getTournamentSpectator(duelist1 to duelist2)
                for (participant in others) {
                    tournamentService.setSpectator(participant)
                    PaperLib.teleportAsync(participant.player, map.spawnLocation)
                }
            }
        }

        return CompletableFuture.completedFuture(null)
    }
    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.Deathmatch)
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<*> {
        Log.info("Exiting $key state")

        participantService.freezers().clear()
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until deathmatch!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime <= 5 -> true
        else -> false
    }

    private fun teleportDeathmatchSpawns(): CompletableFuture<Void> {
        val map = mapService.getPlayingMap()
        mapService.getPlayingWorld()?.let { world ->
            val players = participantService.players().values.toList()
            val futures1 = map.deathmatchSpawns
                .mapNotNull { LocationUtils.getLocationFromString(it) }
                .zip(players)
                .map { (location, participant) ->
                    PaperLib.teleportAsync(participant.player, location).thenAccept {
                        participantService.freezers().putIfAbsent(participant.player.uniqueId, location)
                    }
                }

            val futures2 = participantService.spectators().values.map { PaperLib.teleportAsync(it.player, world.spawnLocation) }
            return CompletableFuture.allOf(*futures1.toTypedArray(), *futures2.toTypedArray())
        }
        return CompletableFuture.completedFuture(null)
    }
}