package com.glacier.survivalgames.domain.state

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.extension.setSpectator
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.math.max

@InjectableComponent
class StatePreFFADeathmatch(stateMachine: StateMachine<GameState>,
                            context: GameContext,
                            participantService: GameParticipantService,
                            mapService: GameMapService
) : StateBase(stateMachine, GameState.PreDeathmatch, context, participantService, mapService) {

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.pre-deathmatch", 10)
    }

    override fun enterAsync(): CompletableFuture<Any> {
        super.enterAsync()

        RxBus.listen<PlayerMoveEvent>().subscribe(this::onMove).addTo(disposable)

        Bukkit.broadcastMessage(Chat.message("&4Please allow &8[&e$remainTime&8] &4seconds for all players to load the map."))
        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {

        broadcast()
        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.FFADeathmatch)
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until deathmatch!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
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

    override fun onPickupItem(e: PlayerPickupItemEvent) {
        if (context.spectators.contains(e.player)) {
            e.isCancelled = true
        }
    }

    private fun onMove(e: PlayerMoveEvent) {
        if (e.player.gameParticipant.deathmatchPosition < 0) {
            return
        }

        val index = max(e.player.gameParticipant.deathmatchPosition - 1, 0)
        LocationUtils.getLocationFromString(mapService.playingMap.deathmatchSpawns[index])?.let {
            val current = e.player.location
            if (current.blockX != it.blockX || current.blockZ != it.blockZ) {
                it.pitch = current.pitch
                it.yaw = current.yaw
                PaperLib.teleportAsync(e.player, it)
            }
        }
    }

    private fun onDeathByPlayer(player: Player, damager: Player? = null) {
        val location = player.location
        location.world.strikeLightningEffect(location)

        context.players.removeIf { it.uniqueId == player.uniqueId }
        context.spectators.add(player)

        player.sendMessage(Chat.message("&aYou have been eliminated from the games."))
        Bukkit.broadcastMessage(Chat.message("&6A cannon has be heard in the distance&8.", prefix = false))

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