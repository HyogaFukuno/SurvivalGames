package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.service.SponsorService
import io.fairyproject.container.InjectableComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@InjectableComponent
class SponsorServiceImpl : SponsorService {

    private val sponsors = mutableMapOf<Player, Player>()

    override fun open(player: Player, target: Player) {

    }

    override fun sponsorItem(player: Player, item: ItemStack, amount: Int) {

    }
}