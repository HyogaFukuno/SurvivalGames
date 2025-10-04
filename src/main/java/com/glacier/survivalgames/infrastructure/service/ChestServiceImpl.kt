package com.glacier.survivalgames.infrastructure.service

import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.utils.WeightedList
import io.fairyproject.container.InjectableComponent
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

@InjectableComponent
class ChestServiceImpl : ChestService {
    companion object {
        const val MIN_ITEM_COUNT = 4
        const val MAX_ITEM_COUNT = 7
    }

    private val tier1Materials = WeightedList<Material>().apply {
        // 基本武器・道具（高確率）
        add(Material.ARROW, 1.4)
        add(Material.WOOD_AXE, 2.0)
        add(Material.STONE_AXE, 1.6)
        add(Material.WOOD_SWORD, 2.0)
        add(Material.STONE_SWORD, 1.4)
        add(Material.BOW, 1.0)

        // 基本アイテム（中確率）
        add(Material.FLINT, 2.0)
        add(Material.FEATHER, 2.0)
        add(Material.FISHING_ROD, 1.4)
        add(Material.BOWL, 1.5)
        add(Material.STICK, 3.0)

        // 金インゴット（低確率）
        add(Material.GOLD_INGOT, 1.0)

        // 革防具（中確率）
        add(Material.LEATHER_HELMET, 1.2)
        add(Material.LEATHER_CHESTPLATE, 1.0)
        add(Material.LEATHER_LEGGINGS, 1.2)
        add(Material.LEATHER_BOOTS, 1.4)

        // 食料（高確率）
        add(Material.RAW_FISH, 2.5)
        add(Material.APPLE, 2.0)
        add(Material.RAW_CHICKEN, 2.0)
        add(Material.BREAD, 3.0)
        add(Material.RAW_BEEF, 1.8)
        add(Material.PUMPKIN_PIE, 1.0)
        add(Material.COOKIE, 2.0)
    }

    private val tier2Materials = WeightedList<Material>().apply {
        // 基本アイテム（中確率）
        add(Material.STICK, 2.0)
        add(Material.BOW, 1.5)
        add(Material.ARROW, 2.0)
        add(Material.STONE_SWORD, 1.5)
        add(Material.FLINT_AND_STEEL, 1.0)
        add(Material.BOAT, 0.8)

        // 貴重な材料（低確率）
        add(Material.IRON_INGOT, 1.0)
        add(Material.DIAMOND, 0.4)

        // 上位防具（低確率）
        add(Material.GOLD_HELMET, 0.9)
        add(Material.GOLD_CHESTPLATE, 0.7)
        add(Material.GOLD_LEGGINGS, 0.9)
        add(Material.GOLD_BOOTS, 0.8)
        add(Material.CHAINMAIL_HELMET, 0.6)
        add(Material.CHAINMAIL_CHESTPLATE, 0.4)
        add(Material.CHAINMAIL_LEGGINGS, 0.6)
        add(Material.CHAINMAIL_BOOTS, 0.7)
        add(Material.IRON_HELMET, 0.8)
        add(Material.IRON_CHESTPLATE, 0.8)
        add(Material.IRON_LEGGINGS, 0.8)
        add(Material.IRON_BOOTS, 1.0)

        // 上質な食料（中確率）
        add(Material.GOLDEN_CARROT, 0.8)
        add(Material.GOLDEN_APPLE, 0.4)
        add(Material.MUSHROOM_SOUP, 1.2)
        add(Material.BAKED_POTATO, 2.0)
        add(Material.COOKED_BEEF, 1.8)
        add(Material.COOKED_CHICKEN, 1.8)
    }

    private val tier1Chests = mutableSetOf<Location>()
    private val tier2Chests = mutableSetOf<Location>()

    override fun tier1Chests(): MutableSet<Location> = tier1Chests
    override fun tier2Chests(): MutableSet<Location> = tier2Chests

    override fun getChest(location: Location): Chest?
            = location.block.state as? Chest

    override fun castChest(location: Location): Chest? {
        location.block.type = Material.CHEST
        location.block.state.update(false, false)
        return location.block.state as? Chest
    }

    override fun fillTier1Chest(chest: Chest?) { chest?.let { fill(it, tier1Materials) } }

    override fun fillTier2Chest(chest: Chest?) { chest?.let { fill(it, tier2Materials) } }

    private fun fill(chest: Chest, materials: WeightedList<Material>) {

        val inv = chest.inventory
        inv.clear()

        val random = Random.nextInt(MAX_ITEM_COUNT - MIN_ITEM_COUNT)
        val numItem = if (inv.size > (9 * 3)) random + MAX_ITEM_COUNT * 2
        else random + MIN_ITEM_COUNT + 1

        val selectedMaterials = materials.randomUnique(numItem)
        for (i in 0 until numItem) {

            val material = selectedMaterials[i]
            val slot = recursionSlotIndex(inv)
            val itemStack = ItemStack(material, getStackCount(material))

            inv.setItem(slot, itemStack)
        }
    }

    private fun recursionSlotIndex(inv: Inventory): Int {
        val slot = Random.nextInt(inv.size)
        if (inv.getItem(slot) != null) {
            return recursionSlotIndex(inv)
        }
        return slot
    }

    private fun getStackCount(material: Material): Int = when (material) {
        Material.ARROW -> 5
        Material.COOKIE -> 2
        else -> 1
    }
}