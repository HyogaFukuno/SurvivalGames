package com.glacier.survivalgames.domain.model.state

import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.service.ChestService
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

class StateLiveGame(stateMachine: StateMachine<GameState>, val participantService: GameParticipantService, val chestService: ChestService) : StateMachine.State<GameState>(stateMachine, GameState.LiveGame) {

    var fastDeathmatch = false

    init {
        remainTime = BukkitPlugin.INSTANCE.config.getInt("remain-time.live-game")
    }

    override fun enterAsync(): CompletableFuture<*> {
        Log.info("Entering $key state")
        GameContext.state = key

        chestService.tier2Chests().forEach {
            chestService.fillTier2Chest(chestService.castChest(it))
        }
        Bukkit.broadcastMessage(Chat.message("&3The game have begun!"))
        return CompletableFuture.completedFuture(null)
    }

    override fun update(): TaskResponse<Boolean> {
        Log.info("Update $key state")

        val size = participantService.players().size
        if (size <= 1) {
            stateMachine.sendEvent(GameState.EndGame)
        } else if (!fastDeathmatch && size <= 3) {
            remainTime = 60
            fastDeathmatch = true
        }
        broadcast()

        when (remainTime) {
            1 -> {
                remainTime = 0
                stateMachine.sendEvent(GameState.PreDeathmatch)
            }
            (60 * 17) -> {
                chestService.tier2Chests().forEach { chestService.fillTier2Chest(chestService.castChest(it)) }
                chestService.tier1Chests().forEach { chestService.fillTier1Chest(chestService.castChest(it)) }

                val tributes = participantService.players().values.joinToString { it.player.displayName }
                Bukkit.broadcastMessage(Chat.message("&3Sponsors have refilled the chests!"))
                Bukkit.broadcastMessage(Chat.message("These tributes have passed: $tributes"))
            }
            else -> {
                remainTime--
            }
        }
        return TaskResponse.continueTask()
    }

    override fun exitAsync(): CompletableFuture<*> {
        Log.info("Exiting $key state")
        return CompletableFuture.completedFuture(null)
    }

    override fun broadcast() {
        if (shouldBroadcast()) {
            val time = if (remainTime > 60) remainTime / 60 else remainTime
            val message = if (remainTime > 60) "minutes" else if (remainTime > 1) "seconds" else "second"
            Bukkit.broadcastMessage(Chat.message("&8[&e$time&8] &c$message until deathmatch!"))
        }
    }

    override fun shouldBroadcast(): Boolean = when {
        remainTime > 60 && remainTime % (60 * 5) == 0 -> true

        remainTime <= 60 && remainTime % 60 == 0 -> true
        remainTime <= 60 && remainTime == 30 -> true
        remainTime <= 60 && remainTime == 10 -> true
        remainTime <= 60 && remainTime <= 5 -> true
        else -> false
    }
}