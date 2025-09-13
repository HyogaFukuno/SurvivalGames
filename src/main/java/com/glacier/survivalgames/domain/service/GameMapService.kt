package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameMap


interface GameMapService {
    fun votableMaps(): List<GameMap>
    fun currentVotes(): MutableMap<GameMap, Int>
    fun playingGameMap(): GameMap

    fun add(map: GameMap)
}