package com.glacier.survivalgames.utils

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor

object TextColor {
    fun text(string: String) = ChatColor.translateAlternateColorCodes('&', string)

    fun component(string: String) = LegacyComponentSerializer.legacyAmpersand().deserialize(string)
}