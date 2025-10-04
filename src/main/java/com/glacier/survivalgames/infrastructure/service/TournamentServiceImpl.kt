package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.model.DeathmatchStyle
import com.glacier.survivalgames.domain.model.event.VoteDeathmatchStyleEvent
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@InjectableComponent
class TournamentServiceImpl : TournamentService {
    private data class InventoryData(val contents: Array<ItemStack>, val helmet: ItemStack?, val chestplate: ItemStack?, val leggings: ItemStack?, val boots: ItemStack?) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InventoryData
            return contents.contentEquals(other.contents)
        }

        override fun hashCode(): Int {
            return contents.contentHashCode()
        }
    }

    private var initialized = false
    private val styleVotes = mutableListOf<DeathmatchStyle>()
    private val tributes = mutableSetOf<Player>()
    private val spectators = mutableSetOf<Player>()
    private val inventories = mutableMapOf<Player, InventoryData>()
    private var duelists: Pair<Player, Player>? = null

    override fun decideStyle(): DeathmatchStyle = styleVotes.groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: DeathmatchStyle.FFA

    init {
        RxBus.listen<VoteDeathmatchStyleEvent>().subscribe { x -> styleVotes.add(x.style) }
    }

    override fun setTournamentPlayers(players: List<Player>) {
        if (initialized) {
            return
        }

        initialized = true
        players.forEach {
            tributes.add(it)
            saveInventory(it)
        }
    }

    override fun getNextDuelists(): Pair<Player, Player> {
        val list = tributes.shuffled()

        duelists = list[0] to list[1]
        spectators.remove(duelists?.first)
        spectators.remove(duelists?.second)

        loadInventory(duelists!!.first)
        loadInventory(duelists!!.second)

        return duelists!!
    }

    override fun getDuelists(): Pair<Player, Player>? = duelists

    override fun removeTournamentPlayers(player: Player) {
        tributes.removeIf { it.uniqueId == player.uniqueId }
        inventories.remove(player)
    }

    override fun saveInventory(player: Player?) {
        if (player == null) {
            return
        }

        inventories[player] = InventoryData(player.inventory.contents.clone(),
            player.inventory.helmet?.clone(),
            player.inventory.chestplate?.clone(),
            player.inventory.leggings?.clone(),
            player.inventory.boots?.clone())
    }

    override fun setTournamentSpectator(player: Player) { spectators.add(player) }

    override fun isTournamentSpectator(player: Player): Boolean = spectators.contains(player)

    private fun loadInventory(player: Player) {
        inventories[player]?.let {
            player.health = 20.0
            player.foodLevel = 20
            player.inventory.contents = it.contents
            player.inventory.helmet = it.helmet
            player.inventory.chestplate = it.chestplate
            player.inventory.leggings = it.leggings
            player.inventory.boots = it.boots
        }
    }
}