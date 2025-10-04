package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.model.GameParticipant
import com.glacier.survivalgames.domain.model.Participant
import com.glacier.survivalgames.domain.repository.ParticipantRepository
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player

@InjectableComponent
class GameParticipantServiceImpl(val repository: ParticipantRepository): GameParticipantService {
    override fun create(player: Player): GameParticipant {
        repository.findByUniqueId(player.uniqueId)?.let {
            val profile = GameParticipant.create(player, it)
            profile.rank = repository.getRanking(player.uniqueId)
            return profile
        }
        val profile = GameParticipant.create(player, Participant.create(player))
        profile.rank = repository.getRanking(player.uniqueId)
        repository.save(profile.participant)
        return profile
    }

    override fun save(player: Player) {
        repository.save(player.gameParticipant.participant.copy(
            points = player.gameParticipant.points,
            kills = player.gameParticipant.kills,
            chests = player.gameParticipant.chests,
            wins = player.gameParticipant.wins,
            played = player.gameParticipant.played,
            lifespan = player.gameParticipant.lifespan,
            previousMap = player.gameParticipant.previousMap
        ))
    }
}