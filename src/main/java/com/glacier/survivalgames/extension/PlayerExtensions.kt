package com.glacier.survivalgames.extension

import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameParticipant
import com.glacier.survivalgames.domain.model.Participant
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

fun Player.reset() {
    health = maxHealth
    foodLevel = 20
    level = 0
    exp = 0.0F

    inventory.clear()
    inventory.helmet = null
    inventory.chestplate = null
    inventory.leggings = null
    inventory.boots = null

    gameMode = GameMode.SURVIVAL
    allowFlight = false
    isFlying = false

    activePotionEffects.forEach { removePotionEffect(it.type) }
}

fun Player.spectator() {
    allowFlight = true
    Bukkit.getOnlinePlayers().forEach { it.hidePlayer(this) }
}

val Player.gameParticipant: GameParticipant?
    get() = GameContext.gameParticipants.firstOrNull { it.uuid == uniqueId }

val Player.participant: Participant?
    get() = gameParticipant?.participant