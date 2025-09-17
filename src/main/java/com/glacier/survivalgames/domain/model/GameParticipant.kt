package com.glacier.survivalgames.domain.model

import org.bukkit.entity.Player
import java.util.UUID

class GameParticipant(val player: Player,
                      val participant: Participant,
                      var position: Int,
                      var kills: Int,
                      var wins: Int,
                      var played: Int,
                      var chests: Int,
                      var points: Int,
                      var lifespan: Int,
                      var bounties: Int,
                      var rank: Int) {
    companion object {

        fun create(player: Player, participant: Participant) = GameParticipant(
            player,
            participant,
            position = 0,
            kills = participant.kills,
            wins = participant.wins,
            played = participant.played,
            chests = participant.chests,
            points = participant.points,
            lifespan = participant.lifespan,
            bounties = 0,
            rank = 0)
    }
}

val GameParticipant.uniqueId: UUID
    get() = participant.uniqueId