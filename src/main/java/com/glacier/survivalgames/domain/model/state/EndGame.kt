package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import java.util.concurrent.CompletableFuture

class EndGame(stateMachine: StateMachine<GameState>) : StateMachine.State<GameState>(stateMachine, GameState.EndGame) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.end-game")
    }

    override fun enterAsync(): CompletableFuture<Void> {
        Log.info("Entering $key state")
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

    override fun exitAsync(): CompletableFuture<Void> {
        Log.info("Exiting $key state")
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {}

    override fun shouldBroadcast(): Boolean = false
}