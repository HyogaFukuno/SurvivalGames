package com.glacier.survivalgames.domain.model.event

import com.glacier.survivalgames.domain.model.DeathmatchStyle
import org.bukkit.Material
import org.bukkit.entity.Player

data class SponsorEvent(val player: Player, val type: Material, val price: Int)
data class OpenDeathmatchMenuEvent(val player: Player)
data class VoteDeathmatchStyleEvent(val player: Player, val style: DeathmatchStyle)