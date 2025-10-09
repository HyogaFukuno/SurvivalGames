package com.glacier.survivalgames.presentation.sidebar

import com.glacier.survivalgames.domain.StateMachine
import com.glacier.survivalgames.domain.entity.GameContext
import com.glacier.survivalgames.domain.entity.GameState
import com.glacier.survivalgames.utils.TextColor
import io.fairyproject.container.InjectableComponent
import io.fairyproject.mc.MCPlayer
import io.fairyproject.sidebar.SidebarLine
import io.fairyproject.sidebar.SidebarProvider
import net.kyori.adventure.text.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@InjectableComponent
class MySidebarProvider(val stateMachine: StateMachine<GameState>, val context: GameContext) : SidebarProvider {
    companion object {
        val FormatterDateOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        val FormatterTimeOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a z", Locale.ENGLISH)
    }

    override fun getTitle(mcPlayer: MCPlayer): Component {
        val name = stateMachine.currentState?.key?.name
        val minutes = stateMachine.currentState?.remainTime?.div(60).toString() // div は割り算
        val seconds = stateMachine.currentState?.remainTime?.mod(60).toString().padStart(2, '0') // mod は余剰計算

        return TextColor.component("&a$name &c$minutes:$seconds")
    }

    override fun getLines(mcPlayer: MCPlayer): List<SidebarLine> {
        val now = ZonedDateTime.now()
        val country = Locale.getDefault().country
        val dateOnly = FormatterDateOnly.format(now)
        val timeOnly = FormatterTimeOnly.format(now)
        val playing = context.players.size
        val watching = context.spectators.size

        return listOf(
            SidebarLine.of(TextColor.component("&6&l» Time")),
            SidebarLine.of(TextColor.component(dateOnly)),
            SidebarLine.of(TextColor.component(timeOnly)),
            SidebarLine.of(Component.empty()),
            SidebarLine.of(TextColor.component("&6&l» Server")),
            SidebarLine.of(TextColor.component("&3$country&8: &r1")),
            SidebarLine.of(Component.empty()),
            SidebarLine.of(TextColor.component("&6&l» Players")),
            SidebarLine.of(TextColor.component("Playing: $playing")),
            SidebarLine.of(TextColor.component("Watching: $watching"))
        )
    }
}