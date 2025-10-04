package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setSpectator
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.service.TournamentService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.container.InjectableComponent
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.ItemStack
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import kotlin.collections.contains
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@InjectableComponent
class StateTournamentDeathmatch(stateMachine: StateMachine<GameState>,
                                context: GameContext,
                                participantService: GameParticipantService,
                                mapService: GameMapService,
                                val tournamentService: TournamentService
) : StateBase(stateMachine, GameState.Deathmatch, context, participantService, mapService) {

    var taskLightning: ScheduledTask<*>? = null
    var taskFinishLightning: ScheduledTask<*>? = null
    var safeArea = 1200

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.deathmatch", 10)
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()

        RxBus.listen<PlayerDamageByPlayerEvent>().subscribe(this::onDamageByPlayer).addTo(disposable)

        val center = LocationUtils.getLocationFromString(mapService.playingMap.deathmatchCenter)
        taskLightning = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate({
            context.players.forEach { player ->
                val distance = center?.distanceSquared(Location(center.world, player.location.x, center.y, player.location.z))
                if (distance != null && distance > safeArea) {
                    center.world.strikeLightning(player.location)
                }
            }
        }, 1L, 20L * 3)

        Bukkit.broadcastMessage(Chat.message("&cFight to the death!"))
        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {
        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                val next = if (context.players.size <= 1) GameState.EndGame else GameState.PreTournamentDeathmatch
                stateMachine.sendEvent(next)
            }
            else -> {
                remainTime--
                safeArea--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<Any> {
        if (context.players.size <= 1) {
            return super.exitAsync()
        }

        taskLightning?.cancel()
        val duelists = tournamentService.getDuelists()
        taskFinishLightning = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
            if (!context.spectators.contains(duelists?.first)
                && !context.spectators.contains(duelists?.second)) {
                val locationA = duelists?.first?.location
                locationA?.world?.strikeLightning(locationA)

                val locationB = duelists?.second?.location
                locationB?.world?.strikeLightning(locationB)

                return@Callable TaskResponse.continueTask()
            }
            return@Callable TaskResponse.success(true)
        }, 1L, 60L)

        return taskFinishLightning!!.future.thenComposeAsync {
            CompletableFuture.allOf(wonRound(), super.exitAsync()).thenApply {  }
        }
    }

    private fun wonRound(): CompletableFuture<Any> {
        if (context.players.size > 1) {
            val duelists = tournamentService.getDuelists()
            val winner = if (context.spectators.contains(duelists?.first)) duelists?.second else duelists?.first
            Bukkit.broadcastMessage(Chat.message("${winner?.displayName} &ahas won the Tournament Deathmatch!!"))

            var time = 10
            val task: ScheduledTask<Any> = MCSchedulers.getGlobalScheduler().scheduleAtFixedRate(Callable {
                if (time < 1) {

                    tournamentService.saveInventory(winner)
                    return@Callable TaskResponse.success(Unit)
                }
                if (time == 10 || time <= 5) {
                    Bukkit.broadcastMessage(Chat.message("&cThe next tournament will in &8[&e$time&8]&c seconds!"))
                }
                time--
                return@Callable TaskResponse.continueTask()
            }, 0L, 20L)

            return task.future
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until the deathmatch ends!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime % 60 == 0 -> true
        remainTime == 30 -> true
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

        if (context.players.removeIf { it.uniqueId == e.player.uniqueId }) {
            onDeathByPlayer(e.player, context.lastAttacked[e.player])
        }
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

    override fun onDamage(e: PlayerDamageEvent) {
        if (context.players.size <= 1) {
            e.isCancelled = true
            return
        }

        if (context.spectators.contains(e.player) || tournamentService.isTournamentSpectator(e.player)) {
            e.isCancelled = true
            return
        }

        if (e.player.health - e.finalDamage <= 0.1) {
            taskFinishLightning?.cancel()
            onDeathByPlayer(e.player, context.lastAttacked[e.player])
            context.lastAttacked[e.player]?.let { it.health = min(it.health + 0.2, 20.0) }
            val next = if (context.players.size <= 1) GameState.EndGame else GameState.PreTournamentDeathmatch
            stateMachine.sendEvent(next)
        }
    }

    override fun onFoodChange(e: FoodLevelChangeEvent) {
        if (context.spectators.contains(e.entity) || tournamentService.isTournamentSpectator(e.entity as Player)) {
            e.isCancelled = true
        }
    }

    override fun onPickupItem(e: PlayerPickupItemEvent) {
        if (context.spectators.contains(e.player) || tournamentService.isTournamentSpectator(e.player)) {
            e.isCancelled = true
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

    override fun onEnterVehicle(e: VehicleEnterEvent) {
        if (context.spectators.contains(e.entered) || tournamentService.isTournamentSpectator(e.entered as Player)) {
            e.isCancelled = true
        }
    }

    private fun onDamageByPlayer(e: PlayerDamageByPlayerEvent) {
        if (context.spectators.contains(e.player)
            || context.spectators.contains(e.damager)
            || tournamentService.isTournamentSpectator(e.damager)) {
            e.isCancelled = true
            return
        }

        context.lastAttacked[e.player] = e.damager
        if (context.lastAttackedTask.containsKey(e.player)) {
            val task = context.lastAttackedTask[e.player]
            task?.cancel()
        }
        context.lastAttackedTask[e.player] = MCSchedulers.getGlobalScheduler().schedule({ context.lastAttacked.remove(e.player) }, 20L * 10)
    }

    private fun onDeathByPlayer(player: Player, damager: Player? = null) {
        val location = player.location.clone()
        location.world.strikeLightningEffect(location)

        context.players.removeIf { it.uniqueId == player.uniqueId }
        context.spectators.add(player)
        tournamentService.removeTournamentPlayers(player)

        damager?.gameParticipant?.deathmatchPosition = -1
        damager?.inventory?.addItem(ItemStack(Material.GOLDEN_APPLE))
        damager?.sendMessage(Chat.message("&aYou've gained a golden apple for win rewards!"))

        player.sendMessage(Chat.message("&aYou have been eliminated from the games."))
        Bukkit.broadcastMessage(Chat.message("&6A cannon has be heard in the distance&8.", prefix = false))

        deathTask(player, location)

        // 倒されたプレイヤーはポイントをドロップする
        val participant = player.gameParticipant
        val drop = max(participant.points / 20, 5)
        participant.points = max(participant.points - drop, 0)
        player.sendMessage(Chat.message("&3You've lost &8[&e${drop}&8] &3points for dying&8!"))

        // 倒したプレイヤーはポイントをゲットする
        damager?.gameParticipant?.points += drop
        damager?.gameParticipant?.kills++
        damager?.sendMessage(Chat.message("&3You've gained &8[&e$drop&8] &3points for killing ${player.displayName}&8!"))

        // 倒されたプレイヤーが他プレイヤーにかけられていたら
        // 倒したプレイヤーにそのポイントを加算する
        if (participant.bounties > 0) {
            val bounties = participant.bounties
            damager?.gameParticipant?.points += bounties
            participant.bounties = 0

            damager?.sendMessage(Chat.message("&3You've gained &8[&e$bounties&8] &3extra points from bounties set on ${player.displayName}&8."))
            Bukkit.broadcastMessage(Chat.message("&6A bounty of &8[&a$bounties&8] &6points has been claimed upon ${player.displayName}&6's death&8."))
        }
    }
}