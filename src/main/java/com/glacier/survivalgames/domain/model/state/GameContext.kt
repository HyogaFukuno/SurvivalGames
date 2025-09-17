package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState

object GameContext {
    var state = GameState.Lobby
    var deathmatchType = DeathmatchType.FFA
}