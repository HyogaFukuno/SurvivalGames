package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.model.Bounty
import com.glacier.survivalgames.domain.service.BountyService
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player
import kotlin.math.max

@InjectableComponent
class BountyServiceImpl : BountyService {

    /**
     * Key: Player = かけた人
     * Value: Pair<Player, Int> = かけられた人と数
     */
    private val bounties = mutableListOf<Bounty>()
    private val confirmBounties = mutableMapOf<Player, Pair<Player, Int>>()

    override fun contains(player: Player): Boolean = confirmBounties.containsKey(player)

    override fun addBounty(player: Player, target: Player, amount: Int) {
        confirmBounties[player] = target to amount
    }

    override fun confirmBounty(player: Player): Pair<Player, Int>? {
        if (!confirmBounties.containsKey(player)) {
            return null
        }

        val (target, amount) = confirmBounties.getValue(player)
        bounties.add(Bounty(player, target, amount))

        player.gameParticipant.points = max(player.gameParticipant.points - amount, 0)
        target.gameParticipant.bounties += amount

        return target to amount
    }

    override fun getBounties(player: Player): List<Bounty> = bounties.filter { it.target == player }
    override fun getBountiesFromTarget(target: Player): List<Bounty> = bounties.filter { it.target == target }
}