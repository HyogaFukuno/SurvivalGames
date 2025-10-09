package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.application.service.ParticipantService
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameParticipant
import com.glacier.survivalgames.domain.entity.removeCacheIf
import com.glacier.survivalgames.extension.gameParticipant
import com.glacier.survivalgames.extension.getNullable
import com.glacier.survivalgames.extension.reset
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.RxBus
import com.glacier.survivalgames.utils.max
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.log.Log
import io.fairyproject.mc.MCPlayer
import io.fairyproject.mc.scheduler.MCSchedulers
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.let
import kotlin.math.min

abstract class StateBase(stateMachine: StateMachine<GameState>,
                         key: GameState,
                         protected val context: GameContext,
                         protected val audienceProvider: AudienceProvider,
                         protected val participantService: ParticipantService,
                         protected val mapService: GameMapService
) : StateMachine.State<GameState>(stateMachine, key) {

    companion object {
        protected const val ONE_MINUTES = 60
        @JvmStatic
        protected val POINT_FORMATTER = ThreadLocal.withInitial { NumberFormat.getIntegerInstance(Locale.US) }

        @JvmStatic
        protected val CPU_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        @JvmStatic
        protected val IO_POOL = Executors.newFixedThreadPool(2)

        private val CHANCE_FORMAT = DecimalFormat("0.##")
    }

    protected val disposables = CompositeDisposable()

    override fun enterAsync(): CompletableFuture<Void> {
        context.state = key

        Log.info("called ${key}.enterAsync")

        RxBus.listen<PlayerJoinEvent>().subscribe(this::onJoin).addTo(disposables)
        RxBus.listen<PlayerQuitEvent>().subscribe(this::onQuit).addTo(disposables)
        RxBus.listen<AsyncPlayerChatEvent>().subscribe(this::onChat).addTo(disposables)
        RxBus.listen<PlayerDamageEvent>().subscribe(this::onDamage).addTo(disposables)
        RxBus.listen<PlayerDamageByPlayerEvent>().subscribe(this::onDamageByPlayer).addTo(disposables)
        RxBus.listen<PlayerMoveEvent>().subscribe(this::onMove).addTo(disposables)
        RxBus.listen<PlayerPickupItemEvent>().subscribe(this::onItemPickup).addTo(disposables)
        RxBus.listen<VehicleEnterEvent>().subscribe(this::onEnterVehicle).addTo(disposables)
        RxBus.listen<FoodLevelChangeEvent>().subscribe(this::onFoodChange).addTo(disposables)
        RxBus.listen<PlayerInteractEvent>().subscribe(this::onInteract).addTo(disposables)

        return CompletableFuture.completedFuture(null)
    }

    override fun exitAsync(): CompletableFuture<Void> {
        disposables.clear()
        return CompletableFuture.completedFuture(null)
    }

    protected fun broadcastVoteMessage() {
        MCPlayer.Companion.BRIDGE.all().forEach { sendVoteMessage(it) }
        sendVoteMessage(audienceProvider.console())
    }

    protected fun sendVoteMessage(audience: Audience) {
        val players = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()

        audience.sendMessage { Chat.component("&2Players waiting &8[&6$players&8/&6$maxPlayers&8]. &2Game requires &8[&6${context.settings.requiredPlayers}&8] &2to play.") }
        audience.sendMessage { Chat.component("&2Vote using &8[&a/vote #&8].") }

        audience.getNullable(Identity.UUID)?.let {
            val player = Bukkit.getPlayer(it)
            audience.sendMessage { Chat.component("&2Previous maps played&8: &7${player.gameParticipant?.previousMap}") }
        }

        val totalVotes = mapService.currentVotes.values.sum()
        var index = 1
        mapService.currentVotes.forEach { (map, votes) ->
            val chance = if (totalVotes > 0) votes.toDouble() / totalVotes * 100.0 else 0.0
            audience.sendMessage { Chat.component("&a${index++}  &8>  | &e$votes &7Votes &8| &e${CHANCE_FORMAT.format(chance)}% &7Chance &8| &2${map.name}") }
        }
    }

    private fun onJoin(e: PlayerJoinEvent) {
        Log.info("called ${key}.onJoin")
        CompletableFuture.runAsync({
            e.player.reset()
            GameContext.gameParticipants.add(participantService.create(e.player))

            val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
            audienceProvider.all().sendMessage { Chat.component("&8[&e${points}&8]&r${e.player.displayName} &6has joined&8.", prefix = false) }
            onJoin(e.player)
        }, CPU_POOL).exceptionally { it.printStackTrace(); null }
    }

    private fun onQuit(e: PlayerQuitEvent) {
        CompletableFuture.runAsync({
            context.players.removeIf { e.player.uniqueId == it }
            context.spectators.removeIf { e.player.uniqueId == it }
            context.removeCacheIf { e.player.uniqueId == it }

            val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
            audienceProvider.all().sendMessage { Chat.component("&8[&e${points}&8]&r${e.player.displayName} &6has left&8.", prefix = false) }

            onQuit(e.player)
        }, CPU_POOL).exceptionally { it.printStackTrace(); null }
    }

    protected open fun onChat(e: AsyncPlayerChatEvent) {
        val points = POINT_FORMATTER.get().format(e.player.gameParticipant?.points)
        audienceProvider.all().sendMessage { Chat.component("&8[&e$points&8]&r${e.player.displayName}&8: &r${e.message}", prefix = false) }
    }

    protected open fun onDamage(e: PlayerDamageEvent) { e.isCancelled = true }
    protected open fun onDamageByPlayer(e: PlayerDamageByPlayerEvent) { e.isCancelled = true }
    protected open fun onMove(e: PlayerMoveEvent) { e.isCancelled = true }
    protected open fun onItemPickup(e: PlayerPickupItemEvent) { e.isCancelled = true }
    protected open fun onEnterVehicle(e: VehicleEnterEvent) { e.isCancelled = true }
    protected open fun onFoodChange(e: FoodLevelChangeEvent) { e.isCancelled = true }
    protected open fun onInteract(e: PlayerInteractEvent) { e.isCancelled = true }

    protected open fun onJoin(player: Player) {}

    protected open fun onQuit(player: Player) {}

    protected fun damage(e: PlayerDamageEvent) {
        if (context.players.size <= 1
            || context.spectators.contains(e.player.uniqueId)){
            e.isCancelled = true
            return
        }

        if (e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return
        }

        e.damage = min(e.damage, 19.99)
        if (e.player.health - e.damage <= 0.1) {
            val damager = Bukkit.getPlayer(context.lastAttacked[e.player.uniqueId])
            onDeathByPlayer(e.player, damager)
            if (context.players.size <= 1) {
                damager?.let { it.health = min(it.health + 0.2, it.maxHealth) }
                stateMachine.sendEvent(GameState.EndGame)
            }
        }
    }

    protected fun damageByPlayer(e: PlayerDamageByPlayerEvent) {
        if (context.spectators.contains(e.player.uniqueId)
            || context.spectators.contains(e.damager.uniqueId)) {
            e.isCancelled = true
            return
        }

        context.lastAttacked[e.player.uniqueId] = e.damager.uniqueId
        context.lastAttackedTask[e.player.uniqueId]?.cancel()
        context.lastAttackedTask[e.player.uniqueId] = MCSchedulers.getAsyncScheduler().schedule({
            context.lastAttacked.remove(e.player.uniqueId)
        }, 20L * 5)

        e.damage = min(e.damage, 19.99)
        if (e.player.health - e.damage <= 0.1) {
            onDeathByPlayer(e.player, e.damager)
            if (context.players.size <= 1) {
                e.damager.health = min(e.damager.health + 0.2, e.damager.maxHealth)
                stateMachine.sendEvent(GameState.EndGame)
            }
        }
    }

    protected fun onDeathByPlayer(player: Player, damager: Player? = null) {
        val location = player.location.clone()

        context.players.removeIf { it == player.uniqueId }
        context.spectators.add(player.uniqueId)

        audienceProvider.player(player).sendMessage { Chat.component("&aYou have been eliminated from the games.") }
        if (context.state == GameState.LiveGame) {
            audienceProvider.all().sendMessage { Chat.component("&aOnly &8[&6${context.players.size}&8]&a tributes remain!") }
            audienceProvider.all().sendMessage { Chat.component("&aThere are &8[&6${context.spectators.size}&8]&a spectators watching the game.") }
        }
        audienceProvider.all().sendMessage { Chat.component("&6A cannon has be heard in the distance&8.", prefix = false) }

        death(player, location)

        // 倒されたプレイヤーはポイントをドロップする
        val drop = max(player.gameParticipant?.points?.div(20), GameParticipant.MINIMUM_DROP_POINTS)
        player.gameParticipant?.points = max(player.gameParticipant?.points?.minus(drop), 0)
        audienceProvider.player(player).sendMessage { Chat.component("&3You've lost &8[&e$drop&8]&3 points for dying&8!") }

        // 倒したプレイヤーはポイントをゲットする
        damager?.let {
            it.gameParticipant?.points += drop
            it.gameParticipant?.kills++
            audienceProvider.player(it).sendMessage { Chat.component("&3You've gained &8[&e$drop&8]&3 points for killing ${player.displayName}&8!") }
        }

        // 倒されたプレイヤーが他プレイヤーにかけられていたら
        // 倒したプレイヤーにそのポイントを加算する
        player.gameParticipant?.bounties?.let { bounties ->
            if (bounties > 0) {
                val cache = player.gameParticipant?.bounties
                player.gameParticipant?.bounties = 0

                damager?.let {
                    it.gameParticipant?.points += cache ?: 0
                    audienceProvider.player(it).sendMessage { Chat.component("&3You've gained &8[&e$cache&8] &3extra points from bounties set on ${player.displayName}&8.") }
                }
                audienceProvider.all().sendMessage { Chat.component("&6A bounty of &8[&a$cache&8] &6points has been claimed upon ${player.displayName}&6's death&8.") }
            }
        }
    }

    protected fun death(player: Player, location: Location) {
        location.add(0.5, 0.5, 0.5)

        player.allowFlight = true
        player.health = player.maxHealth
        player.foodLevel = 20

        MCSchedulers.getGlobalScheduler().schedule {
            location.world.strikeLightningEffect(location)

            player.health = player.maxHealth
            player.foodLevel = 20

            Bukkit.getOnlinePlayers().forEach { it.hidePlayer(player) }
            PaperLib.teleportAsync(player, location).thenAccept {
                player.inventory.filterNotNull().forEach {
                    player.world.dropItemNaturally(player.location, it)
                }
                player.inventory.helmet?.let { player.world.dropItemNaturally(player.location, it) }
                player.inventory.chestplate?.let { player.world.dropItemNaturally(player.location, it) }
                player.inventory.leggings?.let { player.world.dropItemNaturally(player.location, it) }
                player.inventory.boots?.let { player.world.dropItemNaturally(player.location, it) }
                player.inventory.clear()
                player.inventory.helmet = null
                player.inventory.chestplate = null
                player.inventory.leggings = null
                player.inventory.boots = null
            }
        }
    }
}