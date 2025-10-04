package com.glacier.survivalgames

import com.glacier.survivalgames.infrastructure.manager.DatabaseManager
import io.fairyproject.FairyLaunch
import io.fairyproject.container.Autowired
import io.fairyproject.log.Log
import io.fairyproject.plugin.Plugin

@FairyLaunch
class SurvivalGames : Plugin() {

    @Autowired
    lateinit var databaseManager: DatabaseManager

    override fun onPluginEnable() {
        Log.info("[MCSG] Plugin Enabled.")
   }

    override fun onPluginDisable() {
        databaseManager.close()
    }
}