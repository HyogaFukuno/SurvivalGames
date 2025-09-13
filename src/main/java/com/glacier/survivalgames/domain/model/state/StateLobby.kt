package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import java.util.concurrent.CompletableFuture
import kotlin.compareTo
import kotlin.div
import kotlin.rem

class StateLobby(stateMachine: StateMachine<GameState>,
                 val participantService: GameParticipantService,
                 val mapService: GameMapService) : StateMachine.State<GameState>(stateMachine, GameState.Lobby) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.lobby")
    }

    override fun enterAsync(): CompletableFuture<Void> {
        Log.info("Entering $key state")
        return CompletableFuture.completedFuture(null)
    }
    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.PreGame)
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Void> {
        Log.info("Exiting $key state")

        val map = mapService.playingGameMap()
        WorldCreator(map.worldName).createWorld().apply {
            isAutoSave = false
            time = 1000L

            val players = participantService.players().values.map { it.player }.toList()
            val spawns = map.spawns.shuffled()

            val futures = spawns
                .mapNotNull { LocationUtils.getLocationFromString(it) }
                .zip(players)
                .mapIndexed { i, (location, player) ->
                    PaperLib.teleportAsync(player, location)
                }

            return CompletableFuture.allOf(*futures.toTypedArray())
        }
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until lobby ends!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime == 10 -> true
        remainTime <= 5 -> true
        else -> false
    }
}