package com.glacier.survivalgames.application.service

import com.glacier.survivalgames.domain.entity.GameParticipant
import com.glacier.survivalgames.domain.model.Participant
import com.glacier.survivalgames.domain.repository.ParticipantRepository
import com.glacier.survivalgames.extension.gameParticipant
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player

@InjectableComponent
class ParticipantService(val repository: ParticipantRepository) {
    fun create(player: Player): GameParticipant {
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

    fun save(player: Player) {
        player.gameParticipant?.let {
            repository.save(it.participant.copy(
                points = it.points,
                kills = it.kills,
                chests = it.chests,
                wins = it.wins,
                played = it.played,
                lifespan = it.lifespan,
                previousMap = it.previousMap
            ))
        }
    }
}