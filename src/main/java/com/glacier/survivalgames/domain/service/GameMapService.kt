package com.glacier.survivalgames.domain.service

import com.glacier.survivalgames.domain.model.GameMap

interface GameMapService {
    val maps: List<GameMap>
    val votableMaps: List<GameMap>
    val currentVotes: MutableMap<GameMap, Int>
    val playingMap: GameMap
}