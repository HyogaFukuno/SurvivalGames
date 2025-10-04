package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.Bounty
import org.bukkit.entity.Player

// TODO: Sponsor, Voting deathmatch style

interface BountyService {
    fun contains(player: Player): Boolean
    fun addBounty(player: Player, target: Player, amount: Int)
    fun confirmBounty(player: Player): Pair<Player, Int>?
    fun getBounties(player: Player): List<Bounty>
    fun getBountiesFromTarget(target: Player): List<Bounty>
}