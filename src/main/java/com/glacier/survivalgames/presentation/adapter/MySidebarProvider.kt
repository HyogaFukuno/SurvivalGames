package com.glacier.survivalgames.presentation.adapter

import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.StateMachine
import io.fairyproject.container.InjectableComponent
import io.fairyproject.mc.MCPlayer
import io.fairyproject.sidebar.SidebarLine
import io.fairyproject.sidebar.SidebarProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@InjectableComponent
class MySidebarProvider(val stateMachine: StateMachine<GameState>, val gameContext: GameContext) : SidebarProvider {

    companion object {
        val FormatterDateOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        val FormatterTimeOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a z", Locale.ENGLISH)
    }

    override fun getTitle(mcPlayer: MCPlayer): Component {
        val name = stateMachine.currentState?.key?.name
        val minutes = stateMachine.currentState?.remainTime?.div(60).toString() // div は割り算
        val seconds = stateMachine.currentState?.remainTime?.mod(60).toString().padStart(2, '0') // mod は余剰計算

        return LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize("&a$name &c$minutes:$seconds")
    }

    override fun getLines(mcPlayer: MCPlayer): List<SidebarLine> {
        val now = ZonedDateTime.now()
        val country = Locale.getDefault().country
        val dateOnly = FormatterDateOnly.format(now)
        val timeOnly = FormatterTimeOnly.format(now)
        val playing = gameContext.players.size
        val watching = gameContext.spectators.size

        return listOf(
            SidebarLine.of(text("&6&l» Time")),
            SidebarLine.of(text(dateOnly)),
            SidebarLine.of(text(timeOnly)),
            SidebarLine.of(text("")),
            SidebarLine.of(text("&6&l» Server")),
            SidebarLine.of(text("&3$country&8: &r1")),
            SidebarLine.of(text("")),
            SidebarLine.of(text("&6&l» Players")),
            SidebarLine.of(text("Playing: $playing")),
            SidebarLine.of(text("Watching: $watching"))
        )
    }

    override fun shouldDisplay(mcPlayer: MCPlayer): Boolean = true

    private fun text(string: String): Component
        = LegacyComponentSerializer.legacyAmpersand().deserialize(string)
}