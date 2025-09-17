package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameParticipant
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

interface GameParticipantService {
    fun players(): Map<UUID, GameParticipant>
    fun spectators(): Map<UUID, GameParticipant>
    fun freezers(): MutableMap<UUID, Location>

    fun addPlayer(player: Player): GameParticipant
    fun addSpectator(player: Player): GameParticipant

    fun get(player: Player?): GameParticipant?
    fun remove(player: Player): GameParticipant?
    fun save(participant: GameParticipant)
}