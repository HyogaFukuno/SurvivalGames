package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.utils.Chat
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture
import kotlin.compareTo
import kotlin.div
import kotlin.rem

class Deathmatch(stateMachine: StateMachine<GameState>) : StateMachine.State<GameState>(stateMachine, GameState.Deathmatch) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.deathmatch")
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
                stateMachine.sendEvent(GameState.EndGame)
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

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until the deathmatch ends!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime <= 5 -> true
        else -> false
    }
}