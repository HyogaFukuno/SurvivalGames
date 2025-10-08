package com.glacier.survivalgames.domain.message

import org.bukkit.entity.Player

data class OpenServerManagementMessage(val player: Player)

data class OpenMapSelectionMessage(val player: Player)