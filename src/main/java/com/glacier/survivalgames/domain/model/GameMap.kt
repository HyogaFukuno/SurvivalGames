package com.glacier.survivalgames.domain.model

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import java.util.HashMap
import kotlin.collections.Map

@SerializableAs("GameMap")
data class GameMap(val name: String,
                   val worldName: String,
                   val author: String,
                   val url: String,
                   val spawns: List<String>,
                   val deathmatchSpawns: List<String>,
                   val deathmatchCenter: String) : ConfigurationSerializable {

    override fun serialize(): Map<String?, Any?> {
        val data: MutableMap<String?, Any?> = HashMap<String?, Any?>()

        data["name"] = name
        data["world_name"] = worldName
        data["author"] = author
        data["url"] = url
        data["spawns"] = spawns
        data["deathmatch_spawns"] = deathmatchSpawns
        data["deathmatch_center"] = deathmatchCenter
        return data
    }

    override fun toString(): String {
        return "Map (name='$name', worldName='$worldName', author='$author')"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun deserialize(args: Map<String, Any>): GameMap = GameMap(
            args["name"] as String,
            args["world_name"] as String,
            args["author"] as String,
            args["url"] as String,
            args["spawns"] as List<String>,
            args["deathmatch_spawns"] as List<String>,
            args["deathmatch_center"] as String
        )
    }
}

val GameMap.spawnLocation: Location
    get() {
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            return world.spawnLocation
        }

        return WorldCreator(worldName).createWorld().spawnLocation
    }