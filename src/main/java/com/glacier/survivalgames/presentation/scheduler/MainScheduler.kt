package com.glacier.survivalgames.presentation.scheduler

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.state.StateCleanup
import com.glacier.survivalgames.domain.model.state.StateDeathmatch
import com.glacier.survivalgames.domain.model.state.StateEndGame
import com.glacier.survivalgames.domain.model.state.StateLiveGame
import com.glacier.survivalgames.domain.model.state.StatePreDeathmatch
import com.glacier.survivalgames.domain.model.state.StateLobby
import com.glacier.survivalgames.domain.model.state.StatePreGame
import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PostInitialize
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import java.util.concurrent.Callable


@InjectableComponent
class MainScheduler(val stateMachine: StateMachine<GameState>,
                    val participantService: GameParticipantService,
                    val mapService: GameMapService,
                    val chestService: ChestService) {

    lateinit var scheduledTask: ScheduledTask<Boolean>

    @PreInitialize
    fun onPreInitialize() {
        Log.info("[MCSG] onPreInitialize")

        stateMachine.registerState(StateLobby(stateMachine, participantService, mapService))
        stateMachine.registerState(StatePreGame(stateMachine, participantService, mapService, chestService))
        stateMachine.registerState(StateLiveGame(stateMachine, participantService, mapService, chestService))
        stateMachine.registerState(StatePreDeathmatch(stateMachine, participantService))
        stateMachine.registerState(StateDeathmatch(stateMachine))
        stateMachine.registerState(StateEndGame(stateMachine, participantService, mapService))
        stateMachine.registerState(StateCleanup(stateMachine))
        stateMachine.setStartState(GameState.Lobby)
    }

    @PostInitialize
    fun onPostInitialize() {
        Log.info("[MCSG] onPostInitialize")
        executeTask()
    }

    private fun executeTask() {
        scheduledTask = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable{
            stateMachine.update()
        }, 10L, 20L)
    }

    @PreDestroy
    fun onPreDestroy() { scheduledTask.cancel() }
}