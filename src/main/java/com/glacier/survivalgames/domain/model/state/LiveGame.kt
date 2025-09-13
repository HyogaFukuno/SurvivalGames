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

class LiveGame(stateMachine: StateMachine<GameState>) : StateMachine.State<GameState>(stateMachine, GameState.LiveGame) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.live-game")
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
                stateMachine.sendEvent(GameState.PreDeathmatch)
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
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until deathmatch!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime > 60 && remainTime % (60 * 5) == 0 -> true

        remainTime <= 60 && remainTime % 60 == 0 -> true
        remainTime <= 60 && remainTime == 30 -> true
        remainTime <= 60 && remainTime == 10 -> true
        remainTime <= 60 && remainTime <= 5 -> true
        else -> false
    }
}