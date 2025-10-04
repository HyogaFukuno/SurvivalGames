package com.glacier.survivalgames.domain.model

import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.fairyproject.log.Log
import io.fairyproject.scheduler.ScheduledTask
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent

@InjectableComponent
class GameContext(val participantFactory: GameParticipantService) {
    val players = mutableListOf<Player>()
    val spectators = mutableListOf<Player>()
    val lastAttacked = mutableMapOf<Player, Player>()
    val lastAttackedTask = mutableMapOf<Player, ScheduledTask<*>>()
    var state = GameState.Lobby
    var deathmatchStyle = DeathmatchStyle.FFA
    private val disposable = CompositeDisposable()

    companion object {
        internal val gameParticipants = mutableListOf<GameParticipant>()
    }

    @PreInitialize
    fun onPreInitialize() {
        RxBus.listen<PlayerJoinEvent>().subscribe {
            gameParticipants.add(participantFactory.create(it.player))
        }.addTo(disposable)
    }

    @PreDestroy
    fun onPreDestroy() {
        disposable.clear()
    }
}