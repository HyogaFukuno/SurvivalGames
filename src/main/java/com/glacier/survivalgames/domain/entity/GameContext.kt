package com.glacier.survivalgames.domain.entity

import io.fairyproject.container.InjectableComponent
import io.fairyproject.mc.MCPlayer
import io.fairyproject.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@InjectableComponent
class GameContext {
    companion object {
        val gameParticipants: MutableSet<GameParticipant> = Collections.newSetFromMap(ConcurrentHashMap(36))
    }

    val settings by lazy { GameSettings() }
    var state = GameState.Lobby
    var deathmatchStyle = DeathmatchStyle.FFA

    // 最大参加人数で容量を設定しておく（メモリ最適化）
    val players: MutableSet<UUID> = Collections.newSetFromMap(ConcurrentHashMap(32))

    // 全プレイヤーが観戦者になることはない
    val spectators: MutableSet<UUID> = Collections.newSetFromMap(ConcurrentHashMap(32))

    val lastAttacked = ConcurrentHashMap<UUID, UUID>(32)
    val lastAttackedTask = ConcurrentHashMap<UUID, ScheduledTask<*>>(32)


    internal val bukkitCache = ConcurrentHashMap<UUID, Player>(48)
    internal val mcCache = ConcurrentHashMap<UUID, MCPlayer>(48)
}

fun GameContext.removeCacheIf(predicate: (UUID) -> Boolean) {
    bukkitCache.entries.removeIf { predicate(it.key) }
    mcCache.entries.removeIf { predicate(it.key) }
}

fun GameContext.getMCPlayers(): Set<MCPlayer> = players
    .asSequence()
    .mapNotNull { uuid ->
        mcCache[uuid] ?: MCPlayer.from(uuid)?.also { mcCache[uuid] = it }
    }
    .toSet()

fun GameContext.getMCSpectators(): Set<MCPlayer> = spectators
    .asSequence()
    .mapNotNull { uuid ->
        mcCache[uuid] ?: MCPlayer.from(uuid)?.also { mcCache[uuid] = it }
    }
    .toSet()

fun GameContext.getBukkitPlayers(): Set<Player> = players
    .asSequence()
    .mapNotNull { uuid ->
        bukkitCache[uuid] ?: Bukkit.getPlayer(uuid)?.also { bukkitCache[uuid] = it }
    }
    .toSet()

fun GameContext.getBukkitSpectators(): Set<Player> = spectators
    .asSequence()
    .mapNotNull { uuid ->
        bukkitCache[uuid] ?: Bukkit.getPlayer(uuid)?.also { bukkitCache[uuid] = it }
    }
    .toSet()