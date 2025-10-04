package com.glacier.survivalgames.domain.model

import io.fairyproject.bootstrap.bukkit.BukkitPlugin

object GameSettings {
    var requiredPlayers = BukkitPlugin.INSTANCE.config.getInt("settings.required-players", 1)
    var privacy = PrivacyType.PUBLIC
    val mutators = Mutators()
}

class Mutators {
    var gameLength = GameLengthType.THIRTY_MINUTES
    var daylightCycle = true
    var naturalRegeneration = true
    var gracePeriod = GracePeriodType.ZERO
    var maxHealth = MaxHealthType.HEARTS_20
    var chestTiering = ChestTieringType.DEFAULT
}


enum class PrivacyType(val value: Int, val displayName: String) {
    PUBLIC(0, "Public"),
    PRIVATE(1, "Private")
}




enum class GameLengthType(val value: Int, val displayName: String) {
    TEN_MINUTES(60 * 10, "10 minutes"),
    FIFTEEN_MINUTES(60 * 15, "15 minutes"),
    TWENTY_MINUTES(60 * 20, "20 minutes"),
    TWENTY_FIVE_MINUTES(60 * 25, "25 minutes"),
    THIRTY_MINUTES(60 * 30, "30 minutes"),
}

enum class GracePeriodType(val value: Int, val displayName: String) {
    ZERO(0, "0:00 (Disabled)"),
    ONE_MINUTES(1, "1:00"),
    TWO_MINUTES(2, "2:00"),
    THREE_MINUTES(3, "3:00"),
    FOUR_MINUTES(4, "4:00")
}

enum class MaxHealthType(val value: Double, val displayName: String) {
    HEARTS_3(3.0  , "3 hearts"),
    HEARTS_5(5.0  , "5 hearts"),
    HEARTS_10(10.0, "10 hearts"),
    HEARTS_15(15.0, "15 hearts"),
    HEARTS_20(20.0, "20 hearts"),
    HEARTS_30(30.0, "25 hearts")
}

enum class ChestTieringType(val displayName: String) {
    DEFAULT("Default"),
    ALL_TIER1("All Tier 1"),
    ALL_TIER2("All Tier 2")
}