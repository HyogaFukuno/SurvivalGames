package com.glacier.survivalgames.utils

import org.bukkit.Bukkit
import org.bukkit.Location

object LocationUtils {

    fun getStringFromLocation(loc: Location?): String {
        if (loc == null) {
            return ""
        }
        return """${loc.world.name}:${loc.x}:${loc.y}:${loc.z}:${loc.yaw}:${loc.pitch}"""
    }

    fun getLocationFromString(s: String?): Location? {
        if (s == null || s.trim { it <= ' ' } === "") {
            return null
        }
        val parts: Array<String?> = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size == 6) {
            val w = Bukkit.getServer().getWorld(parts[0]!!)
            val x = parts[1]!!.toDouble()
            val y = parts[2]!!.toDouble()
            val z = parts[3]!!.toDouble()
            val yaw = parts[4]!!.toFloat()
            val pitch = parts[5]!!.toFloat()
            return Location(w, x, y, z, yaw, pitch)
        }
        return null
    }

    fun getLiteStringFromLocation(loc: Location?): String {
        if (loc == null) {
            return ""
        }
        return """${loc.world.name}:${loc.blockX}:${loc.blockY}:${loc.blockZ}"""
    }

    fun getLiteLocationFromString(s: String?): Location? {
        if (s == null || s.trim { it <= ' ' }.isEmpty()) {
            return null
        }
        val parts: Array<String?> = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size == 4) {
            val w = Bukkit.getServer().getWorld(parts[0]!!)
            val x = parts[1]!!.toDouble()
            val y = parts[2]!!.toDouble()
            val z = parts[3]!!.toDouble()
            return Location(w, x, y, z)
        }
        return null
    }
}