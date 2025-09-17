package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.model.GameParticipant
import com.glacier.survivalgames.domain.model.uniqueId
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.service.TournamentService
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import java.util.UUID

class TournamentServiceImpl(val participantService: GameParticipantService) : TournamentService {

    private val items = mutableMapOf<UUID, InventoryCache>()

    override fun getTournamentDuelist(except: GameParticipant?): GameParticipant
            = participantService.players().values.filter { it != except }.random()

    override fun getTournamentSpectator(duelists: Pair<GameParticipant, GameParticipant>): List<GameParticipant>
            = participantService.players().values.filter { it != duelists.first && it == duelists.second }

    override fun saveItemTemporary(participant: GameParticipant) {
        items[participant.uniqueId] = InventoryCache(
                participant.player.inventory.contents.clone(),
                participant.player.inventory.armorContents.clone())

        participant.player.inventory.contents = null
        participant.player.inventory.armorContents = null
    }

    override fun loadItemTemporary(participant: GameParticipant) {
        items.remove(participant.uniqueId)?.let {
            participant.player.inventory.contents = it.contents
            participant.player.inventory.armorContents = it.armorContents
        }
    }

    override fun setDuelist(duelist: GameParticipant) {
        duelist.player.health = 20.0
        duelist.player.foodLevel = 20
        duelist.player.allowFlight = false
        Bukkit.getOnlinePlayers().forEach { it.showPlayer(duelist.player) }
        loadItemTemporary(duelist)
    }

    override fun setSpectator(spectator: GameParticipant) {
        spectator.player.health = 20.0
        spectator.player.foodLevel = 20
        spectator.player.allowFlight = true
        Bukkit.getOnlinePlayers().forEach { it.hidePlayer(spectator.player) }
        saveItemTemporary(spectator)
    }

    private data class InventoryCache(val contents: Array<ItemStack>, val armorContents: Array<ItemStack>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InventoryCache

            if (!contents.contentEquals(other.contents)) return false
            if (!armorContents.contentEquals(other.armorContents)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contents.contentHashCode()
            result = 31 * result + armorContents.contentHashCode()
            return result
        }
    }
}