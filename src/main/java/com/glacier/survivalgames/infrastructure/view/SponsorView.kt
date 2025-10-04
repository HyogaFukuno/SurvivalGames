package com.glacier.survivalgames.infrastructure.view

import com.glacier.survivalgames.domain.model.event.SponsorEvent
import com.glacier.survivalgames.utils.ColorText
import com.glacier.survivalgames.utils.RxBus
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class SponsorView : View() {
    companion object {
        fun gerPrice(type: Material): Int = when (type) {
            Material.ENDER_PEARL -> 600
            Material.IRON_INGOT -> 100
            Material.ARROW -> 200
            Material.EXP_BOTTLE -> 800
            Material.CAKE -> 400
            Material.PORK -> 100
            Material.BOW -> 300
            Material.FLINT_AND_STEEL -> 400
            Material.MUSHROOM_SOUP -> 250
            else -> 0
        }
        private val enderPearl by lazy {
            val item = ItemStack(Material.ENDER_PEARL)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.ENDER_PEARL)}&a points"))
            item.itemMeta = meta
            item
        }

        private val ironIngot by lazy {
            val item = ItemStack(Material.IRON_INGOT)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.IRON_INGOT)}&a points"))
            item.itemMeta = meta
            item
        }

        private val arrow by lazy {
            val item = ItemStack(Material.ARROW)
            item.amount = 5
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.ARROW)}&a points"))
            item.itemMeta = meta
            item
        }

        private val experienceBottle by lazy {
            val item = ItemStack(Material.EXP_BOTTLE)
            item.amount = 2
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.EXP_BOTTLE)}&a points"))
            item.itemMeta = meta
            item
        }

        private val cake by lazy {
            val item = ItemStack(Material.CAKE)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.CAKE)}&a points"))
            item.itemMeta = meta
            item
        }

        private val porkChop by lazy {
            val item = ItemStack(Material.PORK)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.PORK)}&a points"))
            item.itemMeta = meta
            item
        }

        private val bow by lazy {
            val item = ItemStack(Material.BOW)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.BOW)}&a points"))
            item.itemMeta = meta
            item
        }

        private val flintAndSteel by lazy {
            val item = ItemStack(Material.FLINT_AND_STEEL)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.FLINT_AND_STEEL)}&a points"))
            item.itemMeta = meta
            item
        }

        private val mushroomStew by lazy {
            val item = ItemStack(Material.MUSHROOM_SOUP)
            val meta = item.itemMeta
            meta.lore = listOf(ColorText.text("&8[&6Cost&8]&e ${gerPrice(Material.MUSHROOM_SOUP)}&a points"))
            item.itemMeta = meta
            item
        }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title(ColorText.text("&7[&6Sponsors&7]"))
        config.size(1)
    }

    override fun onFirstRender(render: RenderContext) {
        render.availableSlot(enderPearl).cancelOnClick()
        render.availableSlot(ironIngot).cancelOnClick()
        render.availableSlot(arrow).cancelOnClick()
        render.availableSlot(experienceBottle).cancelOnClick()
        render.availableSlot(cake).cancelOnClick()
        render.availableSlot(porkChop).cancelOnClick()
        render.availableSlot(bow).cancelOnClick()
        render.availableSlot(flintAndSteel).cancelOnClick()
        render.availableSlot(mushroomStew).cancelOnClick()
    }

    override fun onClick(ctx: SlotClickContext) {
        val item = ctx.item
        val meta = item.itemMeta
        meta.lore = listOf()
        item.itemMeta = meta

        RxBus.publish(SponsorEvent(ctx.player, item.type, gerPrice(item.type)))
        ctx.closeForPlayer()
    }
}