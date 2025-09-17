package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.utils.Chat
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.fairyproject.scheduler.response.TaskResponse
import org.bukkit.Bukkit
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

class StateDeathmatch(stateMachine: StateMachine<GameState>, val participantService: GameParticipantService) : StateMachine.State<GameState>(stateMachine, GameState.Deathmatch) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.deathmatch")
    }

    override fun enterAsync(): CompletableFuture<*> {
        Log.info("Entering $key state")
        GameContext.state = key

        Bukkit.broadcastMessage(Chat.message("&cFight to the death!"))
        return CompletableFuture.completedFuture(null)
    }
    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                when (GameContext.deathmatchType) {
                    DeathmatchType.FFA -> stateMachine.sendEvent(GameState.EndGame)
                    DeathmatchType.Tournament -> {
                        if (participantService.players().size <= 1) {
                            stateMachine.sendEvent(GameState.EndGame)
                        } else {
                            stateMachine.sendEvent(GameState.PreDeathmatch)
                        }
                    }
                }
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<*> {
        Log.info("Exiting $key state")

        val taskLightning = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
            if (participantService.players().size > 1) {
                for (participant in participantService.players().values) {
                    val location = participant.player.location
                    location.world.strikeLightning(location)
                }
                TaskResponse.continueTask<Boolean>()
            }
            TaskResponse.success(true)
        }, 1L, 60L)

        return when (stateMachine.nextKey) {
            GameState.PreDeathmatch -> {
                var timer = 10
                val task = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
                    if (timer > 0) {
                        val message = if (timer > 1) "seconds" else "second"
                        Bukkit.broadcastMessage(Chat.message("&8[&e$timer&8] &c$message until the another tournament!"))
                        timer--
                    }
                    TaskResponse.success(true)
                }, 1L, 20L)

                return taskLightning.future
            }
            GameState.EndGame -> taskLightning.future
            else -> CompletableFuture.completedFuture(null)
        }
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