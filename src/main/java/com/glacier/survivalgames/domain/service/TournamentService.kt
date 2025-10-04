package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.DeathmatchStyle
import org.bukkit.entity.Player

interface TournamentService {
    fun decideStyle(): DeathmatchStyle
    fun setTournamentPlayers(players: List<Player>)
    fun getNextDuelists(): Pair<Player, Player>
    fun getDuelists(): Pair<Player, Player>?
    fun removeTournamentPlayers(player: Player)
    fun saveInventory(player: Player?)
    fun setTournamentSpectator(player: Player)
    fun isTournamentSpectator(player: Player): Boolean
}