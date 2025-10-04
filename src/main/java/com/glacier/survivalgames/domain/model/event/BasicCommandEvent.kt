package com.glacier.survivalgames.domain.model.event

import org.bukkit.entity.Player

data class CommandVoteEvent(val player: Player, val number: Int?)
data class CommandSponsorEvent(val player: Player, val target: Player)
data class CommandSeppukuEvent(val player: Player)