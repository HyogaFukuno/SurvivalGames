package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameParticipant

interface TournamentService {

    fun getTournamentDuelist(except: GameParticipant? = null): GameParticipant
    fun getTournamentSpectator(duelists: Pair<GameParticipant, GameParticipant>): List<GameParticipant>

    fun saveItemTemporary(participant: GameParticipant)
    fun loadItemTemporary(participant: GameParticipant)

    fun setDuelist(duelist: GameParticipant)
    fun setSpectator(spectator: GameParticipant)
}