package com.glacier.survivalgames.domain.model

import org.bukkit.entity.Player
import java.util.UUID

data class Participant(
    val uuid: UUID,
    val points: Int,
    val wins: Int,
    val played: Int,
    val kills: Int,
    val chests: Int,
    val lifespan: Int,
    val previousMap: String) {
    companion object {
        const val DEFAULT_POINTS = 1000

        fun create(player: Player) = Participant(
            uuid = player.uniqueId,
            points = DEFAULT_POINTS,
            wins = 0,
            played = 0,
            kills = 0,
            chests = 0,
            lifespan = 0,
            previousMap = ""
        )
    }
}
