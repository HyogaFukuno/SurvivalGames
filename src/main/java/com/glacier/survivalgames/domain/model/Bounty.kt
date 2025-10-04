package com.glacier.survivalgames.domain.model

import org.bukkit.entity.Player

data class Bounty(val player: Player, val target: Player, val amount: Int)