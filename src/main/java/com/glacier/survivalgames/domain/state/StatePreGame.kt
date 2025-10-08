package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.ChestService
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.entity.getMCSpectators
import com.glacier.survivalgames.domain.model.GameMap
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.extension.spectator
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.ChunkUtils
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.imanity.imanityspigot.chunk.AsyncPriority
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

class StatePreGame(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   audienceProvider: AudienceProvider,
                   participantService: ParticipantService,
                   mapService: GameMapService,
                   val chestService: ChestService
) : StateBase(stateMachine, GameState.PreGame, context, audienceProvider, participantService, mapService) {

    private val defaultTime by lazy { BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-game", 10) }
    private val maps: Queue<GameMap> by lazy { LinkedList(mapService.maps.filter { it.worldName != mapService.decideMap.worldName }) }
    private val scanExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    init {
        remainTime = defaultTime
    }

    override fun enterAsync(): CompletableFuture<Any> {
        MCSchedulers.getAsyncScheduler().schedule {
            val futures = ConcurrentLinkedQueue<CompletableFuture<Chunk>>()
            val world = Bukkit.getWorld(mapService.decideMap.worldName)
            world.imanity().getChunkAtAsynchronously(world.spawnLocation, AsyncPriority.HIGH)
                .thenCompose { center ->
                    val radius = mapService.decideMap.radius / ChunkUtils.SIZE
                    Log.info("Chunk search radius: $radius")

                    val taskTopLeft = MCSchedulers.getAsyncScheduler().schedule {
                        getChunksAsync(center.x - radius, center.x, center.z - radius, center.z, futures, world)
                    }

                    val taskTopRight = MCSchedulers.getAsyncScheduler().schedule {
                        getChunksAsync(center.x, center.x + radius, center.z - radius, center.z, futures, world)
                    }

                    val taskBottomLeft = MCSchedulers.getAsyncScheduler().schedule {
                        getChunksAsync(center.x - radius, center.x, center.z, center.z + radius, futures, world)
                    }

                    val taskBottomRight = MCSchedulers.getAsyncScheduler().schedule {
                        getChunksAsync(center.x, center.x + radius, center.z, center.z + center.z + radius, futures, world)
                    }

                    CompletableFuture.allOf(
                        taskTopLeft.future,
                        taskTopRight.future,
                        taskBottomLeft.future,
                        taskBottomRight.future)
                }.thenCompose {
                    val chunks = futures.toList()
                    val ft = chunks.map { future -> scanChunkAsync(future.get())
                        .exceptionally { it.printStackTrace(); null }
                    }
                    CompletableFuture.allOf(*ft.toTypedArray())
                }.thenAccept {
                    val length = chestService.tier1Chests.size
                    var index = 0

                    val world = Bukkit.getWorld(mapService.decideMap.worldName)
                    MCSchedulers.getGlobalScheduler().scheduleAtFixedRate({
                        if (index >= length) {
                            TaskResponse.success(true)
                        }
                        else {
                            val (x, y, z) = chestService.tier1Chests.keys.elementAt(index)
                            chestService.fillTier1Chest(chestService.getChest(x, y , z, world))
                            index++
                            TaskResponse.continueTask()
                        }
                    }, 0L, 2L)

                    Log.info("Tier1 chests: ${chestService.tier1Chests.size}")
                    Log.info("Tier2 chests: ${chestService.tier2Chests.size}")
                }.exceptionally { it.printStackTrace(); null }
        }

        return super.enterAsync().thenApply {
            audienceProvider.all().sendMessage { Chat.component("&eMap name&8: &2${mapService.decideMap.name}") }
            audienceProvider.all().sendMessage { Chat.component("&eMap author&8: &2${mapService.decideMap.author}") }
            audienceProvider.all().sendMessage { Chat.component("&eMap link&8: &2${mapService.decideMap.url}") }

            Bukkit.unloadWorld("lobby", false)
        }
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        if (remainTime <= 1) {
            remainTime = 0

            val next = if (context.players.size > 1) GameState.LiveGame else GameState.EndGame
            stateMachine.sendEvent(next)
            return TaskResponse.continueTask()
        }

        remainTime--

        return TaskResponse.continueTask()
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > ONE_MINUTES) remainTime / ONE_MINUTES else remainTime
            val symbols = if (remainTime > ONE_MINUTES) "minutes" else if (remainTime > 1) "seconds" else "second"
            val message = "&8[&e$time&8] &c$symbols until the games begin!"

            audienceProvider.all().sendMessage { Chat.component(message) }
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % ONE_MINUTES == 0 -> true
        remainTime == 30 -> true
        remainTime == 10 -> true
        remainTime <= 5 -> true
        else -> false
    }

    override fun onJoin(player: Player) {
        val world = Bukkit.getWorld(mapService.decideMap.worldName)
        MCSchedulers.getGlobalScheduler().schedule {
            PaperLib.teleportAsync(player, world.spawnLocation).thenAccept {
                player.spectator()
                context.spectators.add(player.uniqueId)
            }
        }
    }

    override fun onChat(e: AsyncPlayerChatEvent) {
        // 発言者が観戦者の場合は観戦者とコンソールのみ送信する
        if (context.spectators.contains(e.player.uniqueId)) {
            val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
            context.getMCSpectators().forEach { it.sendMessage { Chat.component("&8[&e$points&8]&4SPEC&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) } }
            audienceProvider.console().sendMessage { Chat.component("&8[&e$points&8]&4SPEC&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
        }
        // 発言者が生存者の場合は全てのユーザー、コンソールに送信する
        else {
            audienceProvider.all().sendMessage { Chat.component("&8[&a${e.player.gameParticipant?.bounties}&8]&c${e.player.gameParticipant?.position}&8|&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
        }
    }

    override fun onMove(e: PlayerMoveEvent) {
        val position = e.player.gameParticipant?.position
        if (position == null || position == -1) {
            return
        }

        val spawn = mapService.decideMap.spawns[position]
        LocationUtils.getLocationFromString(spawn)?.let {
            if (e.player.location.blockX != it.blockX
                || e.player.location.blockZ != it.blockZ) {

                it.pitch = e.player.location.pitch
                it.yaw = e.player.location.yaw
                PaperLib.teleportAsync(e.player, it)
            }
        }
    }

    private fun unloadWorld(map: GameMap): CompletableFuture<*> {
        return MCSchedulers.getGlobalScheduler().schedule({

            Log.info("Unloading world ${map.worldName}...")
            Bukkit.unloadWorld(map.worldName, false)

            if (maps.isNotEmpty()) {
                unloadWorld(maps.poll())
            }
            else {
                Log.info("All worlds unloaded.")
                CompletableFuture.completedFuture(null)
            }
        }, 20L).future
    }

    @Suppress("DEPRECATION")
    private fun scanChunkAsync(chunk: Chunk?): CompletableFuture<*> {
        if (chunk == null) {
            return CompletableFuture.completedFuture(null)
        }

        val snapshot = chunk.getChunkSnapshot(true, false, false)
        val chunkX = snapshot.x
        val chunkZ = snapshot.z

        val positions = mutableSetOf<Triple<Int, Int, Int>>()
        for (x in 0 until ChunkUtils.SIZE) {
            for (z in 0 until ChunkUtils.SIZE) {
                val maxY = chunk.getHighestBlockYAt(x, z)
                for (y in 0 .. maxY) {
                    positions.add(Triple(x, y, z))
                }
            }
        }

        return CompletableFuture.runAsync({
            positions.parallelStream().forEach { (x, y, z) ->
                val id = snapshot.getBlockTypeId(x, y, z)
                val material = Material.getMaterial(id)
                when (material) {
                    Material.CHEST -> {
                        chestService.tier1Chests[Triple(chunkX * ChunkUtils.SIZE + x, y, chunkZ * ChunkUtils.SIZE + z)] = true
                    }
                    Material.ENDER_CHEST -> {
                        chestService.tier2Chests[Triple(chunkX * ChunkUtils.SIZE + x, y, chunkZ * ChunkUtils.SIZE + z)] = true
                    }
                    else -> {}
                }
            }
        }, scanExecutor)
    }

    private fun getChunksAsync(startX: Int, endX: Int, startZ: Int, endZ: Int, futures: ConcurrentLinkedQueue<CompletableFuture<Chunk>>, world: World) {
        for (x in startX until endX) {
            for (z in startZ until endZ) {
                futures.add(world.imanity().getChunkAtAsynchronously(x, z, AsyncPriority.NORMAL))
            }
        }
    }
}