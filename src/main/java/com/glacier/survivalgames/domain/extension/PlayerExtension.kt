package com.glacier.survivalgames.domain.extension

import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameParticipant
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

val Player.gameParticipant: GameParticipant
    get() = GameContext.gameParticipants.first { it.player.uniqueId == uniqueId }

fun Player.setInitialStatus() {
    gameMode = GameMode.SURVIVAL
    health = 20.0
    foodLevel = 20
    level = 0
    exp = 0.0f

    activePotionEffects.forEach { removePotionEffect(it.type) }
}

fun Player.setSpectator() {
    allowFlight = true
    inventory.clear()
    inventory.helmet = null
    inventory.chestplate = null
    inventory.leggings = null
    inventory.boots = null
    Bukkit.getOnlinePlayers().forEach { it.hidePlayer(this) }
}