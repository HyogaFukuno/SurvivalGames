package com.glacier.survivalgames.presentation.view

import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.domain.message.OpenMapSelectionMessage
import com.glacier.survivalgames.domain.model.GameMap
import com.glacier.survivalgames.utils.RxBus
import com.glacier.survivalgames.utils.TextColor
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.state.State
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class ServerManagementMainView : View() {
    companion object {
        private val MAP_SELECTION by lazy { itemMapSelection() }

        private fun itemMapSelection(): ItemStack {
            val item = ItemStack(Material.MAP)
            item.itemMeta = item.itemMeta.apply {
                displayName = TextColor.text("&bMap Selection")
            }
            return item
        }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title("Manage Server")
        config.size(1)
    }

    override fun onFirstRender(render: RenderContext) {
        render.slot(2, MAP_SELECTION)
    }

    override fun onClick(click: SlotClickContext) {
        click.player.sendMessage("Hello World")
        click.openForPlayer(ServerManagementMapSelectionView::class.java)
    }
}

class ServerManagementMapSelectionView(mapService: GameMapService) : View() {

    private val maps = mapService.maps.toMutableList()
    
    val paginationState: State<Pagination> = paginationState(maps) { context, builder, index, map ->
        builder.withItem(ItemStack(Material.MAP).apply {
            itemMeta = itemMeta?.apply {
                displayName = TextColor.text("&b${map.name}")
                lore = listOf(TextColor.text("&6${map.author}"))
            }
        })
        builder.onClick { ctx -> ctx.player.sendMessage("Hello World") }
    }
    override fun onInit(config: ViewConfigBuilder) {
        config.title("Map Selection")
        config.size(3)

        config.layout(
            "         ",
            "  OOOOO  ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = paginationState.get(render)

        render.slot(0, ItemStack(Material.CLAY_BRICK))
            .updateOnStateChange(paginationState)
            .onClick(pagination::back)

        render.slot(8, ItemStack(Material.CLAY_BRICK))
            .updateOnStateChange(paginationState)
            .onClick(pagination::advance)
    }
}