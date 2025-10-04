package com.glacier.survivalgames.domain.model

import com.glacier.survivalgames.domain.service.BountyService
import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.domain.state.StateCleanup
import com.glacier.survivalgames.domain.state.StateEndGame
import com.glacier.survivalgames.domain.state.StateFFADeathmatch
import com.glacier.survivalgames.domain.state.StateLiveGame
import com.glacier.survivalgames.domain.state.StateLobby
import com.glacier.survivalgames.domain.state.StatePreFFADeathmatch
import com.glacier.survivalgames.domain.state.StatePreGame
import com.glacier.survivalgames.domain.state.StatePreTournamentDeathmatch
import com.glacier.survivalgames.domain.state.StateTournamentDeathmatch
import io.fairyproject.container.InjectableComponent

@InjectableComponent
class GameStateFactory(val stateMachine: StateMachine<GameState>,
                       val context: GameContext,
                       val participantService: GameParticipantService,
                       val mapService: GameMapService,
                       val chestService: ChestService,
                       val bountyService: BountyService,
                       val tournamentService: TournamentService) : StateMachine.StateFactory<GameState> {

    override fun create(key: GameState?): StateMachine.State<GameState>? = when (key) {
        GameState.Lobby -> StateLobby(stateMachine, context, participantService, mapService)
        GameState.PreGame -> StatePreGame(stateMachine, context, participantService, mapService, chestService, tournamentService)
        GameState.LiveGame -> StateLiveGame(stateMachine, context, participantService, mapService, chestService)
        GameState.PreFFADeathmatch -> StatePreFFADeathmatch(stateMachine, context, participantService, mapService)
        GameState.FFADeathmatch -> StateFFADeathmatch(stateMachine, context, participantService, mapService)
        GameState.PreTournamentDeathmatch -> StatePreTournamentDeathmatch(stateMachine, context, participantService, mapService, tournamentService)
        GameState.TournamentDeathmatch -> StateTournamentDeathmatch(stateMachine, context, participantService, mapService, tournamentService)
        GameState.EndGame -> StateEndGame(stateMachine, context, participantService, mapService, bountyService)
        GameState.Cleanup -> StateCleanup(stateMachine, context, participantService, mapService)
        else -> null
    }
}