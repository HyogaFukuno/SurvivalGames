package com.glacier.survivalgames.utils

import org.bukkit.ChatColor

object ColorText {
    fun text(string: String) = ChatColor.translateAlternateColorCodes('&', string)
}