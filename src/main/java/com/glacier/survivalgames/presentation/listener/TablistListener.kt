package com.glacier.survivalgames.presentation.listener

import com.glacier.survivalgames.domain.extension.gameParticipant
import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PostInitialize
import io.fairyproject.container.PreDestroy
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import java.util.UUID

@InjectableComponent
@RegisterAsListener
class TablistListener : Listener {
    companion object {
        val manager: ScoreboardManager by lazy { Bukkit.getScoreboardManager() }
        val board: Scoreboard by lazy { manager.newScoreboard }
    }

    lateinit var task: ScheduledTask<*>
    private val objectives = mutableMapOf<UUID, Objective>()

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val objective = board.registerNewObjective("${e.player.name}_TB", "dummy")
        objective.displayName = ""
        objective.displaySlot = DisplaySlot.PLAYER_LIST

        e.player.scoreboard = board
        objectives.putIfAbsent(e.player.uniqueId, objective)
    }

    @EventHandler
    fun onQuit(e: PlayerJoinEvent) { objectives.remove(e.player.uniqueId) }

    @PostInitialize
    fun onPostInitialize() {
        task = MCSchedulers.getAsyncScheduler().scheduleAtFixedRate({
            Bukkit.getOnlinePlayers().forEach { player ->
                objectives[player.uniqueId]?.let { objective ->
                    val score = objective.getScore(player.name)
                    score.score = player.gameParticipant.bounties
                }
            }
        }, 10L, 20L)
    }

    @PreDestroy
    fun onPreDestroy() { task.cancel() }
}