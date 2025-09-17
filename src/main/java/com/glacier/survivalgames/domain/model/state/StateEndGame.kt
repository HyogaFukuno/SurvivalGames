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
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import java.util.concurrent.CompletableFuture

class StateEndGame(stateMachine: StateMachine<GameState>, val participantService: GameParticipantService, val mapService: GameMapService) : StateMachine.State<GameState>(stateMachine, GameState.EndGame) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.end-game")
    }

    override fun enterAsync(): CompletableFuture<*> {
        Log.info("Entering $key state")
        GameContext.state = key

        val map = mapService.getPlayingMap()
        mapService.getPlayingWorld()?.let { world ->
            world.time = 400000L
            map.spawns
                .mapNotNull { LocationUtils.getLocationFromString(it) }
                .forEach { world.spawnEntity(it, EntityType.FIREWORK) }
        }

        val winner = participantService.players().values.firstOrNull()
        winner?.let { winner ->
            winner.wins++
            Bukkit.broadcastMessage(Chat.message("&aThe games have ended!"))
            Bukkit.broadcastMessage(Chat.message("${winner.player.displayName} &ahas won the Survival Games!"))
        }

        return CompletableFuture.completedFuture(null)
    }
    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

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

    override fun exitAsync(): CompletableFuture<*> {
        Log.info("Exiting $key state")
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {}

    override fun shouldBroadcast(): Boolean = false
}