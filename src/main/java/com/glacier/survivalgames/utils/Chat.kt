package com.glacier.survivalgames.utils

import org.bukkit.ChatColor

object Chat {
    private val PREFIX = "&8[&6MCSG&8]&r "

    fun message(msg: String, prefix: Boolean = true): String
            = if (prefix) ColorText.text(PREFIX + msg) else ColorText.text(msg)
}