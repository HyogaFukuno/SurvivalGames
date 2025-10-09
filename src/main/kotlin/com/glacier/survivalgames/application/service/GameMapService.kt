package com.glacier.survivalgames.application.service

import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.model.GameMap
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@InjectableComponent
class GameMapService(val context: GameContext) {
    val maps by lazy { loadMaps() }
    val decideMap by lazy { decideMap() }
    val votableMaps by lazy { maps.shuffled().take(5) }
    val currentVotes by lazy { votableMaps.associateWith { 0 }.toMutableMap() }

    private val file by lazy { File(BukkitPlugin.INSTANCE.dataFolder, "maps.yml") }
    private val config by lazy { YamlConfiguration.loadConfiguration(file) }

    init {
        ConfigurationSerialization.registerClass(GameMap::class.java, "GameMap")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadMaps(): HashSet<GameMap> {
        try {
            if (file.exists()) {
                config.save(file)
            }
            return HashSet(config.getList("maps") as List<GameMap>)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        return hashSetOf()
    }

    private fun decideMap(): GameMap {
        context.settings.gameMap?.let { return it }

        val totalVotes = currentVotes.values.sum()
        if (totalVotes == 0) {
            return votableMaps.random()
        }

        val randomPercent = Math.random() * 100.0
        var cumulativePercent = 0.0

        for ((map, votes) in currentVotes) {
            val chance = votes.toDouble() / totalVotes * 100.0
            cumulativePercent += chance
            if (randomPercent < cumulativePercent) {
                return map
            }
        }
        return votableMaps.random()
    }
}