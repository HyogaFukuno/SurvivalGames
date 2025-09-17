package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.ChunkUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.imanity.imanityspigot.chunk.AsyncPriority
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture

class StatePreGame(stateMachine: StateMachine<GameState>, val participantService: GameParticipantService, val mapService: GameMapService, val chestService: ChestService) : StateMachine.State<GameState>(stateMachine, GameState.PreGame) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-game")
        forgetEnter = true
    }

    override fun enterAsync(): CompletableFuture<*> = CompletableFuture.runAsync {
        Log.info("Entering $key state")
        GameContext.state = key

        participantService.players().values.forEach { it.played++ }
        mapService.getPlayingWorld()?.let {
            val batchSize = 5
            val futures = findChunksAsync(it)
            CompletableFuture.allOf(*futures.toTypedArray()).thenAccept { _ ->
                var task: ScheduledTask<*>? = null
                val chunks: Queue<Chunk> = LinkedList(futures.map { future -> future.join() })
                task = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate({
                    repeat(batchSize) {
                        val chunk = chunks.poll()
                        if (chunk == null) {
                            task?.cancel()
                            return@scheduleAtFixedRate
                        }
                        searchChestAtChunk(chunk)
                    }

                    if (chunks.isEmpty()) {
                        task?.cancel()
                    }
                }, 20L, 1L)
            }
        }

        val map = mapService.getPlayingMap()
        Bukkit.broadcastMessage(Chat.message("&eMap name&8: &2${map.name}"))
        Bukkit.broadcastMessage(Chat.message("&eMap author&8: &2${map.author}"))
        Bukkit.broadcastMessage(Chat.message("&eMap link&8: &2${map.url}"))

        Bukkit.broadcastMessage(Chat.message("&cPlease wait &8[&e$remainTime&8]&c seconds before the games begin!"))
    }

    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.LiveGame)
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<*> {
        Log.info("Exiting $key state")
        participantService.freezers().clear()
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until the games begin!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
        remainTime == 10 -> true
        remainTime <= 5 -> true
        else -> false
    }

    private fun findChunksAsync(world: World): List<CompletableFuture<Chunk>> {
        val futures = mutableListOf<CompletableFuture<Chunk>>()
        world.imanity().getChunkAtAsynchronously(world.spawnLocation, AsyncPriority.HIGHER).thenAccept { center ->
            for (x in center.x - ChunkUtils.SIZE .. center.x + ChunkUtils.SIZE) {
                for (z in center.z - ChunkUtils.SIZE .. center.z + ChunkUtils.SIZE) {
                    futures.add(world.imanity().getChunkAtAsynchronously(x, z, AsyncPriority.HIGHER))
                }
            }
        }

        return futures
    }

    private fun searchChestAtChunk(chunk: Chunk) {
        val chunkX = chunk.x
        val chunkZ = chunk.z

        var findTier1 = 0
        var findTier2 = 0
        for (x in 0 until ChunkUtils.SIZE) {
            for (z in 0 until ChunkUtils.SIZE) {
                for (y in 0 until 256) {
                    val snapshot = PaperLib.getBlockState(chunk.getBlock(x, y, z), false)
                    if (!shouldChestType(snapshot.state.type)) continue

                    val locX = (chunkX * ChunkUtils.SIZE + x).toDouble()
                    val locZ = (chunkZ * ChunkUtils.SIZE + z).toDouble()
                    val locY = y.toDouble()
                    val location = Location(chunk.world, locX, locY, locZ)
                    when (snapshot.state.type) {
                        Material.CHEST -> {
                            chestService.tier1Chests().add(location)
                            chestService.fillTier1Chest(chestService.getChest(location))
                            findTier1++
                        }
                        Material.ENDER_CHEST -> {
                            chestService.tier2Chests().add(location)
                            findTier2++
                        }
                        else -> {}
                    }
                }
            }
        }

        Log.info("Found $findTier1 tier 1 chests and $findTier2 tier 2 chests in chunk $chunkX, $chunkZ")
    }

    private fun shouldChestType(mat: Material): Boolean = mat == Material.CHEST || mat == Material.ENDER_CHEST
}