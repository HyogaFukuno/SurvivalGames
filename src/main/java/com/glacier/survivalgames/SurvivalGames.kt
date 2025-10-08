package com.glacier.survivalgames

import com.glacier.survivalgames.infrastructure.manager.DatabaseManager
import io.fairyproject.FairyLaunch
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.Autowired
import io.fairyproject.log.Log
import io.fairyproject.plugin.Plugin

@FairyLaunch
class SurvivalGames : Plugin() {

    @Autowired
    lateinit var databaseManager: DatabaseManager

    @Autowired
    lateinit var audienceProvider: AudienceProvider

    override fun onPluginEnable() {
        Log.info("[MCSG] Plugin Enabled.")

        BukkitPlugin.INSTANCE.saveDefaultConfig()
        audienceProvider.initialize(BukkitPlugin.INSTANCE)
   }

    override fun onPluginDisable() {
        audienceProvider.close()
        if (databaseManager.use) {
            databaseManager.close()
        }
    }
}