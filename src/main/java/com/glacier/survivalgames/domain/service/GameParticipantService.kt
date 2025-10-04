package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameParticipant
import org.bukkit.entity.Player

interface GameParticipantService {
    fun create(player: Player): GameParticipant
    fun save(player: Player)
}