package com.glacier.survivalgames

import io.fairyproject.container.InjectableComponent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@InjectableComponent
class AudienceProvider {
    lateinit var audiences: BukkitAudiences

    fun initialize(plugin: Plugin) {
        audiences = BukkitAudiences.create(plugin)
    }

    fun close() = audiences.close()

    fun player(player: Player) = audiences.player(player)
    fun all() = audiences.all()
    fun console() = audiences.console()
}