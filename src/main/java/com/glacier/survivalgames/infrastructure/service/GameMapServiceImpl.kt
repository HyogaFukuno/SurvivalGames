package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.model.GameMap
import com.glacier.survivalgames.domain.service.GameMapService
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@InjectableComponent
class GameMapServiceImpl : GameMapService {

    val file: File by lazy { File(BukkitPlugin.INSTANCE.dataFolder, "maps.yml") }
    val config: YamlConfiguration by lazy { YamlConfiguration.loadConfiguration(file) }
    val maps: MutableList<GameMap> by lazy { loadMaps() }

    init {
        ConfigurationSerialization.registerClass(GameMap::class.java, "GameMap")
    }

    private val votableMaps: List<GameMap> by lazy { maps.shuffled().take(5) }
    private val votes: MutableMap<GameMap, Int> by lazy { votableMaps.associateWith { 0 }.toMutableMap() }
    private val chooseMap: GameMap by lazy { votes.maxBy { it.value }.key }

    override fun votableMaps(): List<GameMap> = votableMaps
    override fun currentVotes(): MutableMap<GameMap, Int> = votes
    override fun playingGameMap(): GameMap = chooseMap
    override fun add(map: GameMap) {

        maps.add(map)
        config.set("maps", maps)
        config.save(file)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadMaps(): MutableList<GameMap> {
        try {
            if (file.exists()) {
                config.save(file)
            }
            return config.getList("maps") as MutableList<GameMap>
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        return mutableListOf()
    }
}