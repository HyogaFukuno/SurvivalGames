package com.glacier.survivalgames.infrastructure.view

import com.glacier.survivalgames.utils.ColorText
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder

class GameMasterMainView : View() {

    override fun onInit(config: ViewConfigBuilder) {
        config.title(ColorText.text("&7[&6GameMaster&7]"))
        config.size(1)
    }
}