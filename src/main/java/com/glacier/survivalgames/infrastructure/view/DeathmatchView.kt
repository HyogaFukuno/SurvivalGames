package com.glacier.survivalgames.infrastructure.view

import com.glacier.survivalgames.domain.model.DeathmatchStyle
import com.glacier.survivalgames.domain.model.event.VoteDeathmatchStyleEvent
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.ColorText
import com.glacier.survivalgames.utils.RxBus
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class DeathmatchView : View() {
    companion object {
        private val ffa by lazy {
            val item = ItemStack(Material.BOW)
            val meta = item.itemMeta
            meta.displayName = ColorText.text("&6FFA Style")
            meta.lore = listOf(ColorText.text("&7Click to vote a FFA style."))
            item.itemMeta = meta
            item
        }
        private val tournament by lazy {
            val item = ItemStack(Material.FISHING_ROD)
            val meta = item.itemMeta
            meta.displayName = ColorText.text("&6Tournament Style")
            meta.lore = listOf(ColorText.text("&7Click to vote a Tournament style."))
            item.itemMeta = meta
            item
        }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title(ColorText.text("&7[&6DeathmatchType&7]"))
        config.size(1)
    }

    override fun onFirstRender(render: RenderContext) {
        render.availableSlot(ffa).cancelOnClick()
        render.availableSlot(tournament).cancelOnClick()
    }

    override fun onClick(ctx: SlotClickContext) {
        val style = when (ctx.item?.type) {
            Material.BOW -> DeathmatchStyle.FFA
            Material.FISHING_ROD -> DeathmatchStyle.Tournament
            else -> return
        }

        ctx.player.closeInventory()
        ctx.player.sendMessage(Chat.message("Successfully voted the $style style!"))
        RxBus.publish(VoteDeathmatchStyleEvent(ctx.player, style))
    }
}