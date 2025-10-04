package com.glacier.survivalgames

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.GameStateFactory
import com.glacier.survivalgames.domain.model.StateMachine
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PostInitialize
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import java.util.concurrent.Callable

@InjectableComponent
class MainScheduler(val stateMachine: StateMachine<GameState>, val factory: GameStateFactory) {

    lateinit var scheduledTask: ScheduledTask<*>

    @PreInitialize
    fun onPreInitialize() {
        Log.info("[MCSG] MainScheduler.onPreInitialize")
        stateMachine.setFactory(factory)
        stateMachine.setStartState(GameState.Lobby)
        executeTask()
    }

    private fun executeTask() {
        scheduledTask = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
            stateMachine.update()
        }, 10L, 20L)
    }

    @PreDestroy
    fun onPreDestroy() { scheduledTask.cancel() }
}