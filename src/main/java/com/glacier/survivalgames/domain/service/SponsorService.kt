package com.glacier.survivalgames.domain.service

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface SponsorService {
    fun open(player: Player, target: Player)
    fun sponsorItem(player: Player, item: ItemStack, amount: Int)
}