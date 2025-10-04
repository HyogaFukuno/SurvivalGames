package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.model.GameMap
import com.glacier.survivalgames.domain.service.GameMapService
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

@InjectableComponent
class GameMapServiceImpl : GameMapService {

    override val maps by lazy { loadMaps() }
    override val votableMaps by lazy { maps.shuffled().take(5) }
    override val currentVotes by lazy { votableMaps.associateWith { 0 }.toMutableMap() }
    override val playingMap by lazy { chooseMap() }


    private val file by lazy { File(BukkitPlugin.INSTANCE.dataFolder, "maps.yml") }
    private val config by lazy { YamlConfiguration.loadConfiguration(file) }

    init {
        ConfigurationSerialization.registerClass(GameMap::class.java, "GameMap")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadMaps(): List<GameMap> {
        try {
            if (file.exists()) {
                config.save(file)
            }
            return config.getList("maps") as List<GameMap>
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf()
    }

    private fun chooseMap(): GameMap {
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