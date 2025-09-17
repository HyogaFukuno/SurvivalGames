package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameMap
import org.bukkit.World


interface GameMapService {
    fun votableMaps(): List<GameMap>
    fun currentVotes(): MutableMap<GameMap, Int>
    fun getPlayingMap(): GameMap
    fun getPlayingWorld(): World?

    fun add(map: GameMap)
}