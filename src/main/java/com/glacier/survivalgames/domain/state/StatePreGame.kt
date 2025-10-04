package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setSpectator
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.event.CommandSeppukuEvent
import com.glacier.survivalgames.domain.model.event.OpenDeathmatchMenuEvent
import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.ChunkUtils
import com.glacier.survivalgames.utils.LocationUtils
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.log.Log
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.event.block.Action
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.imanity.imanityspigot.chunk.AsyncPriority
import java.text.NumberFormat
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

@InjectableComponent
class StatePreGame(stateMachine: StateMachine<GameState>,
                   context: GameContext,
                   participantService: GameParticipantService,
                   mapService: GameMapService,
                   val chestService: ChestService,
                   val tournamentService: TournamentService
) : StateBase(stateMachine, GameState.PreGame, context, participantService, mapService) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-game", 10)
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()
        RxBus.listen<PlayerMoveEvent>().subscribe(this::onMove).addTo(disposable)
        RxBus.listen<CommandSeppukuEvent>().subscribe { m -> m.player.health = 0.0 }.addTo(disposable)

        MCSchedulers.getGlobalScheduler().schedule({
            context.players.forEach { RxBus.publish(OpenDeathmatchMenuEvent(it)) }
        }, 10L)

        Bukkit.broadcastMessage(Chat.message("&eMap name&8: &2${mapService.playingMap.name}"))
        Bukkit.broadcastMessage(Chat.message("&eMap author&8: &2${mapService.playingMap.author}"))
        Bukkit.broadcastMessage(Chat.message("&eMap link&8: &2${mapService.playingMap.url}"))

        Bukkit.broadcastMessage(Chat.message("&cPlease wait &8[&e$remainTime&8]&c seconds before the games begin!"))

        return CompletableFuture.supplyAsync {
            val batchSize = 4
            val futures = findChunksAsync(Bukkit.getWorld(mapService.playingMap.worldName))
            CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
                val chunks: Queue<Chunk> = LinkedList(futures.map { it.get() })
                MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
                    if (chunks.isEmpty()) {
                        return@Callable TaskResponse.success(true)
                    }

                    repeat(batchSize) {
                        val chunk = chunks.poll() ?: return@Callable TaskResponse.success(true)
                        searchChestAtChunk(chunk)
                    }
                    return@Callable TaskResponse.continueTask()
                }, 10L, 1L)
            }
        }
    }

    override fun update(): TaskResponse<Boolean> {
        if (remainTime == 10) {
            Bukkit.getOnlinePlayers().forEach { it.player.closeInventory() }
        }

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

    override fun exitAsync(): CompletableFuture<Any> {
        context.deathmatchStyle = tournamentService.decideStyle()
        return super.exitAsync()
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

    override fun onPlayerJoin(e: PlayerJoinEvent) {
        super.onPlayerJoin(e)

        val world = Bukkit.getWorld(mapService.playingMap.worldName)
        PaperLib.teleportAsync(e.player, world.spawnLocation).thenAccept {
            e.player.setSpectator()
            context.spectators.add(e.player)
        }
    }

    override fun onPlayerQuit(e: PlayerQuitEvent) {
        super.onPlayerQuit(e)

        context.players.removeIf { it.uniqueId == e.player.uniqueId }
        context.spectators.removeIf { it.uniqueId == e.player.uniqueId }
    }

    override fun onChat(e: AsyncPlayerChatEvent) {
        val participant = e.player.gameParticipant
        val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)

        if (context.spectators.contains(e.player)) {
            context.players.forEach { e.recipients.remove(it) }
            e.format = Chat.message("&8[&e$points&8]&4SPEC&8|${e.player.displayName}&8: &r${e.message}", prefix = false)
        } else {
            e.format = Chat.message("&8[&a${participant.bounties}&8]&c${participant.position}&8|${e.player.displayName}&8: &r${e.message}", prefix = false)
        }
    }

    override fun onInteract(e: PlayerInteractEvent) {
        if (context.spectators.contains(e.player)) {
            e.isCancelled = true

            if (e.action == Action.RIGHT_CLICK_AIR
                || e.action == Action.RIGHT_CLICK_BLOCK
                || e.action == Action.LEFT_CLICK_BLOCK) {
                val random = context.players.random()
                PaperLib.teleportAsync(e.player, random.location).thenAccept {
                    e.player.sendMessage(Chat.message("Teleporting ${random.displayName}&r."))
                }
            }
        }
    }

    private fun onMove(e: PlayerMoveEvent) {
        val index = e.player.gameParticipant.position - 1
        LocationUtils.getLocationFromString(mapService.playingMap.spawns[index])?.let {
            val current = e.player.location
            if (current.blockX != it.blockX || current.blockZ != it.blockZ) {
                it.pitch = current.pitch
                it.yaw = current.yaw
                PaperLib.teleportAsync(e.player, it)
            }
        }
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
                for (y in 0 until 220) {
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