package com.glacier.survivalgames.presentation.view

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.domain.message.SelectedGameMapMessage
import com.glacier.survivalgames.utils.Chat
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


class ServerManagement {
    class MainView : View() {
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
            config.cancelOnClick()
            config.title(TextColor.text("&rManage Server"))
            config.size(1)
            config.layout("  MPT U  ")
        }

        override fun onFirstRender(render: RenderContext) {
            render.layoutSlot('M')
                .withItem(MAP_SELECTION)
                .onClick { x -> x.openForPlayer(MapSelectionView::class.java) }

            render.layoutSlot('P')
                .withItem(ItemStack(Material.SKULL_ITEM))
                .onClick { x -> x.openForPlayer(PrivacyView::class.java) }

            render.layoutSlot('T')
                .withItem(ItemStack(Material.GOLDEN_APPLE))
                .onClick { x -> x.openForPlayer(MutatorsView::class.java) }
        }
    }

    class MapSelectionView(mapService: GameMapService, audienceProvider: AudienceProvider) : View() {

        private val maps = mapService.maps.toMutableList()

        private val paginationState = buildPaginationState(maps)
            .layoutTarget('O')
            .itemFactory { item, map ->
                item.withItem(ItemStack(Material.MAP).apply {
                    itemMeta = itemMeta?.apply {
                        displayName = TextColor.text("&b${map.name}")
                        lore = listOf(TextColor.text("&6${map.author}"))
                    }
                }).onClick { ctx ->
                    ctx.closeForPlayer()

                    audienceProvider.player(ctx.player).sendMessage { Chat.component("&aThe map has been set.") }
                    RxBus.publish(SelectedGameMapMessage(map))
                }
            }.build()

        private val back by lazy {
            ItemStack(Material.STAINED_CLAY, 1, 1.toShort()).apply {
                itemMeta = itemMeta?.apply {
                    displayName = TextColor.text("&bBack")
                }
            }
        }

        private val next by lazy {
            ItemStack(Material.STAINED_CLAY, 1, 1.toShort()).apply {
                itemMeta = itemMeta?.apply {
                    displayName = TextColor.text("&bNext")
                }
            }
        }

        override fun onInit(config: ViewConfigBuilder) {
            config.cancelOnClick()
            config.title("Map Selection")
            config.size(4)

            config.layout(
                "B       N",
                "  OOOOO  ",
                "  OOOOO  ",
                "         "
            )
        }

        override fun onFirstRender(render: RenderContext) {
            val pagination = paginationState.get(render)

            render.layoutSlot('B')
                .withItem(back)
                .updateOnStateChange(paginationState)
                .displayIf(pagination::canBack)
                .onClick { ctx ->
                    val pagination = paginationState.get(ctx)
                    pagination.back()
                    ctx.update()
                }

            render.layoutSlot('N')
                .withItem(next)
                .updateOnStateChange(paginationState)
                .displayIf(pagination::canAdvance)
                .onClick { ctx ->
                    val pagination = paginationState.get(ctx)
                    pagination.advance()
                    ctx.update()
                }
        }
    }

    class PrivacyView() : View() {
        override fun onInit(config: ViewConfigBuilder) {
            config.cancelOnClick()
            config.title(TextColor.text("&rPrivacy"))
        }
    }

    class MutatorsView() : View() {
        override fun onInit(config: ViewConfigBuilder) {
            config.cancelOnClick()
            config.title(TextColor.text("&rMutators"))
        }
    }
}