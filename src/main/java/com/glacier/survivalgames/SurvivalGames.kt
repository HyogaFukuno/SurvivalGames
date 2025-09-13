package com.glacier.survivalgames

import com.github.retrooper.packetevents.PacketEvents
import io.fairyproject.FairyLaunch
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.plugin.Plugin

@FairyLaunch
class SurvivalGames : Plugin() {

    override fun onInitial() {
        PacketEvents.getAPI().load()
    }

    override fun onPluginEnable() {
        Log.info("[MCSG] Plugin Enabled.")

        PacketEvents.getAPI().init()
    }

    override fun onPluginDisable() {
        PacketEvents.getAPI().terminate()
    }
}