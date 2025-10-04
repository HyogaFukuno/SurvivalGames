package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setInitialStatus
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameSettings
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.event.CommandVoteEvent
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.state.StateLobby.Companion.CHANCE_FORMAT
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture

abstract class StateBase(stateMachine: StateMachine<GameState>, key: GameState, val context: GameContext, val participantService: GameParticipantService, val mapService: GameMapService
) : StateMachine.State<GameState>(stateMachine, key) {

    protected val disposable = CompositeDisposable()

    override fun enterAsync(): CompletableFuture<Any> {
        RxBus.listen<PlayerJoinEvent>().subscribe(this::onPlayerJoin).addTo(disposable)
        RxBus.listen<PlayerQuitEvent>().subscribe(this::onPlayerQuit).addTo(disposable)
        RxBus.listen<AsyncPlayerChatEvent>().subscribe(this::onChat).addTo(disposable)
        RxBus.listen<PlayerDamageEvent>().subscribe(this::onDamage).addTo(disposable)
        RxBus.listen<FoodLevelChangeEvent>().subscribe(this::onFoodChange).addTo(disposable)
        RxBus.listen<PlayerPickupItemEvent>().subscribe(this::onPickupItem).addTo(disposable)
        RxBus.listen<PlayerInteractEvent>().subscribe(this::onInteract).addTo(disposable)
        RxBus.listen<VehicleEnterEvent>().subscribe(this::onEnterVehicle).addTo(disposable)

        RxBus.listen<CommandVoteEvent>().subscribe { if (it.number == null) voteBroadcast() }.addTo(disposable)
        return CompletableFuture.completedFuture(null)
    }

    override fun exitAsync(): CompletableFuture<Any> {
        disposable.clear()
        return CompletableFuture.completedFuture(null)
    }

    protected fun voteBroadcast() {
        val currentPlayers = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()
        Bukkit.broadcastMessage(Chat.message("&2Players waiting &8[&6$currentPlayers&8/&6$maxPlayers&8] &2Game requires &8[&6${GameSettings.requiredPlayers}&8] &2to play."))
        Bukkit.broadcastMessage(Chat.message("&2Vote using &8[&a/vote #&8]."))
        context.players.forEach {
            if (it.gameParticipant.previousMap.isNotEmpty()) {
                it.sendMessage(Chat.message("&2Previous maps played. &7${it.gameParticipant.previousMap}"))
            }
        }
        val totalVotes = mapService.currentVotes.values.sum()
        var count = 1
        mapService.currentVotes.forEach { (map, votes) ->
            val chance = if (totalVotes > 0) votes.toDouble() / totalVotes * 100.0 else 0.0
            Bukkit.broadcastMessage(Chat.message("&a${count++} &8> | &e$votes &7Votes &8| &e${CHANCE_FORMAT.format(chance)}% &7Chance &8| &2${map.name}"))
        }
    }

    protected open fun onPlayerJoin(e: PlayerJoinEvent) {
        e.player.setInitialStatus()
        val participant = e.player.gameParticipant
        val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)

        e.joinMessage = Chat.message("&8[&e${points}&8]${e.player.displayName} &6has joined&8.", prefix = false)
    }

    protected open fun onPlayerQuit(e: PlayerQuitEvent) {
        val participant = e.player.gameParticipant
        val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)

        participantService.save(e.player)
        e.quitMessage = Chat.message("&8[&e${points}&8]${e.player.displayName} &6has left&8.", prefix = false)
    }

    protected open fun onChat(e: AsyncPlayerChatEvent) {
        val participant = e.player.gameParticipant
        val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)

        e.format = Chat.message("&8[&e$points&8]${e.player.displayName}&8: &r${e.message}", prefix = false)
    }

    protected open fun onDamage(e: PlayerDamageEvent) { e.isCancelled = true }

    protected open fun onFoodChange(e: FoodLevelChangeEvent) { e.isCancelled = true }

    protected open fun onPickupItem(e: PlayerPickupItemEvent) { e.isCancelled = true }

    protected open fun onInteract(e: PlayerInteractEvent) { e.isCancelled = true }

    protected open fun onEnterVehicle(e: VehicleEnterEvent) { e.isCancelled = true }

    protected fun deathTask(player: Player, location: Location) {
        val items = player.inventory.contents.clone().toMutableList()
        player.inventory.helmet?.let { items.add(it) }
        player.inventory.chestplate?.let { items.add(it) }
        player.inventory.leggings?.let { items.add(it) }
        player.inventory.boots?.let { items.add(it) }

        player.inventory.clear()
        player.inventory.helmet = null
        player.inventory.chestplate = null
        player.inventory.leggings = null
        player.inventory.boots = null

        player.allowFlight = true
        player.health = player.maxHealth
        player.foodLevel = 20

        val world = location.world
        val loc = location.add(0.5, 0.5, 0.5)
        items.filterNotNull().forEach { world.dropItem(loc, it) }

        MCSchedulers.getGlobalScheduler().schedule {
            Bukkit.getOnlinePlayers().forEach { it.hidePlayer(player) }
            PaperLib.teleportAsync(player, location)
        }
    }
}