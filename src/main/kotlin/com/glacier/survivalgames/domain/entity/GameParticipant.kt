package com.glacier.survivalgames.domain.entity

import com.glacier.survivalgames.domain.model.Participant
import org.bukkit.entity.Player

data class GameParticipant(
    val player: Player,
    val participant: Participant,
    var position: Int,
    var deathmatchPosition: Int,
    var kills: Int,
    var wins: Int,
    var played: Int,
    var chests: Int,
    var points: Int,
    var lifespan: Int,
    var previousMap: String,
    var bounties: Int,
    var rank: Int) {

    val uuid = participant.uuid

    override fun hashCode(): Int = this.uuid.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is GameParticipant) return false
        return uuid == other.uuid
    }

    companion object {
        const val MINIMUM_DROP_POINTS = 5

        fun create(player: Player, participant: Participant) = GameParticipant(
            player,
            participant,
            position = -1,
            deathmatchPosition = -1,
            kills = participant.kills,
            wins = participant.wins,
            played = participant.played,
            chests = participant.chests,
            points = participant.points,
            lifespan = participant.lifespan,
            previousMap = participant.previousMap,
            bounties = 0,
            rank = 0)
    }
}