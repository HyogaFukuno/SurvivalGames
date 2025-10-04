package com.glacier.survivalgames.domain.service

import org.bukkit.Location
import org.bukkit.block.Chest

interface ChestService {
    fun tier1Chests(): MutableSet<Location>
    fun tier2Chests(): MutableSet<Location>

    fun getChest(location: Location): Chest?
    fun castChest(location: Location): Chest?

    fun fillTier1Chest(chest: Chest?)
    fun fillTier2Chest(chest: Chest?)
}