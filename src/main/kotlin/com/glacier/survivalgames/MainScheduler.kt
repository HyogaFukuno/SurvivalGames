package com.glacier.survivalgames

import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.state.GameStateFactory
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PostInitialize
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask

@InjectableComponent
class MainScheduler(val stateMachine: StateMachine<GameState>, val factory: GameStateFactory) {

    private val scheduledTask by lazy { executeTask() }

    @PreInitialize
    fun onPreInitialize() {
        Log.info("[MCSG] MainScheduler.onPreInitialize")

        stateMachine.setFactory(factory)
        stateMachine.setStartState(GameState.Lobby)
    }

    @PostInitialize
    fun onPostInitialize() { scheduledTask.future.exceptionally { it.printStackTrace(); null } }

    @PreDestroy
    fun onPreDestroy() { scheduledTask.close() }

    private fun executeTask(): ScheduledTask<*> = MCSchedulers.getAsyncScheduler().scheduleAtFixedRate({
        stateMachine.update()
    }, 10L, 20L)
}