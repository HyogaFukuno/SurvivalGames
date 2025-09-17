package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.utils.Chat
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

class StateCleanup(stateMachine: StateMachine<GameState>) : StateMachine.State<GameState>(stateMachine, GameState.Cleanup) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.cleanup")
    }

    override fun enterAsync(): CompletableFuture<*> {
        Log.info("Entering $key state")
        GameContext.state = key

        Bukkit.broadcastMessage(Chat.message("&3The server is cleaning up. You will be returned to the lobby in a moment."))
        return CompletableFuture.completedFuture(null)
    }
    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        when (remainTime) {
            1 -> {
                remainTime = 0
                Bukkit.getOnlinePlayers().forEach { it.kickPlayer("Server is restarting") }
                BukkitPlugin.INSTANCE.server.shutdown()
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