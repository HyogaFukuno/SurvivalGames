package com.glacier.survivalgames.utils

import net.kyori.adventure.text.Component

object Chat {
    private const val PREFIX = "&8[&6MCSG&8] &r"

    fun message(msg: String, prefix: Boolean = true): String
            = TextColor.text(if (prefix) PREFIX + msg else msg)

    fun component(msg: String, prefix: Boolean = true): Component
        = TextColor.component(if (prefix) PREFIX + msg else msg)
}