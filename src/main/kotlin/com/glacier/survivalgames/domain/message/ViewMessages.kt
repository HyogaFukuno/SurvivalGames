package com.glacier.survivalgames.domain.message

import com.glacier.survivalgames.domain.model.GameMap
import org.bukkit.entity.Player

data class OpenServerManagementMessage(val player: Player)
data class SelectedGameMapMessage(val map: GameMap)