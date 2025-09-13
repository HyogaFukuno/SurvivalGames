package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.model.GameParticipant
import com.glacier.survivalgames.domain.model.Participant
import com.glacier.survivalgames.domain.repository.ParticipantRepository
import com.glacier.survivalgames.domain.service.GameParticipantService
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

@InjectableComponent
class GameParticipantServiceImpl(private val repository: ParticipantRepository) : GameParticipantService {

    private val players = ConcurrentHashMap<UUID, GameParticipant>()
    private val spectators = ConcurrentHashMap<UUID, GameParticipant>()
    private val freezers = ConcurrentHashMap<UUID, GameParticipant>()

    override fun players(): Map<UUID, GameParticipant> = players
    override fun spectators(): Map<UUID, GameParticipant> = spectators
    override fun freezers(): Map<UUID, GameParticipant> = freezers

    override fun addPlayer(player: Player): GameParticipant {
        repository.findByUniqueId(player.uniqueId)?.let {
            val instance = GameParticipant.create(player, it)
            instance.rank = repository.getRanking(player.uniqueId)

            players[player.uniqueId] = instance
            return instance
        }

        val it = GameParticipant.create(player, Participant.create(player))
        it.rank = repository.getRanking(player.uniqueId)

        players[player.uniqueId] = it
        repository.save(it.participant)

        return it
    }

    override fun addSpectator(player: Player): GameParticipant {
        repository.findByUniqueId(player.uniqueId)?.let {
            val instance = GameParticipant.create(player, it)
            instance.rank = repository.getRanking(player.uniqueId)

            spectators[player.uniqueId] = instance
            return instance
        }

        val it = GameParticipant.create(player, Participant.create(player))
        it.rank = repository.getRanking(player.uniqueId)

        spectators[player.uniqueId] = it
        repository.save(it.participant)

        return it
    }

    override fun get(player: Player?): GameParticipant? {
        player?.let { p ->
            players[p.uniqueId]?.let { return it }
            spectators[p.uniqueId]?.let { return it }
        }
        return null
    }

    override fun remove(player: Player): GameParticipant? {
        players.remove(player.uniqueId)?.let { return it }
        spectators.remove(player.uniqueId)?.let { return it }
        return null
    }

    override fun save(participant: GameParticipant) {
        repository.save(participant.participant
            .copy(
                points = participant.points,
                wins = participant.wins,
                played = participant.played,
                kills = participant.kills,
                chests = participant.chests,
                lifespan = participant.lifespan))
    }


}