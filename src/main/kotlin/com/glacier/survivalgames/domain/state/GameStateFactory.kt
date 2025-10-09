package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.ChestService
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import io.fairyproject.container.InjectableComponent

@InjectableComponent
class GameStateFactory(
    val stateMachine: StateMachine<GameState>,
    val context: GameContext,
    val audienceProvider: AudienceProvider,
    val participantService: ParticipantService,
    val mapService: GameMapService,
    val chestService: ChestService
) : StateMachine.StateFactory<GameState> {

    override fun create(key: GameState?): StateMachine.State<GameState>? = when(key) {
        GameState.Lobby -> StateLobby(stateMachine, context, audienceProvider, participantService, mapService)
        GameState.PreGame -> StatePreGame(stateMachine, context, audienceProvider, participantService, mapService, chestService)
        GameState.LiveGame -> StateLiveGame(stateMachine, context, audienceProvider, participantService, mapService, chestService)
        GameState.PreFFADeathmatch -> StatePreFFADeathmatch(stateMachine, context, audienceProvider, participantService, mapService)
        GameState.FFADeathmatch -> StateFFADeathmatch(stateMachine, context, audienceProvider, participantService, mapService)
        GameState.EndGame -> StateEndGame(stateMachine, context, audienceProvider, participantService, mapService)
        GameState.Cleanup -> StateCleanup(stateMachine, context, audienceProvider, participantService, mapService)
        else -> null
    }
}