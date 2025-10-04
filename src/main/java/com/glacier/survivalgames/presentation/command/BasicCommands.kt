package com.glacier.survivalgames.presentation.command

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.model.GameContext
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.event.CommandSeppukuEvent
import com.glacier.survivalgames.domain.model.event.CommandSponsorEvent
import com.glacier.survivalgames.domain.model.event.CommandVoteEvent
import com.glacier.survivalgames.domain.service.BountyService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bukkit.command.event.BukkitCommandContext
import io.fairyproject.bukkit.command.presence.DefaultPresenceProvider
import io.fairyproject.command.BaseCommand
import io.fairyproject.command.CommandContext
import io.fairyproject.command.MessageType
import io.fairyproject.command.annotation.Arg
import io.fairyproject.command.annotation.Command
import io.fairyproject.command.annotation.CommandPresence
import io.fairyproject.container.InjectableComponent
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player


@InjectableComponent
@Command(value = ["records", "record", "rec", "stats"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandRecords() : BaseCommand() {
    companion object {
        private val luckPermApi by lazy {
            try {
                LuckPermsProvider.get()
            } catch (_: IllegalStateException) {
                null
            }
        }

        private fun show(ctx: CommandContext, player: Player) {
            luckPermApi?.userManager?.getUser(player.uniqueId)?.let {
                val group = luckPermApi?.groupManager?.getGroup(it.primaryGroup)
                ctx.sendMessage(MessageType.INFO, Chat.message("${player.displayName}&r's records"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Rank&8: &e${group?.displayName}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Points&8: &e${player.gameParticipant.points}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Game Rank&8: &e#${player.gameParticipant.rank}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Games (Won/Total)&8: &e${player.gameParticipant.wins}/${player.gameParticipant.played}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Kills (Round/Total)&8: &e${player.gameParticipant.kills - (player.gameParticipant.participant.kills)}/${player.gameParticipant.kills}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Chests (Round/Total)&8: &e${player.gameParticipant.chests - (player.gameParticipant.participant.chests)}/${player.gameParticipant.chests}"))
                ctx.sendMessage(MessageType.INFO, Chat.message("Lifespan (Round/Total)&8: &e${player.gameParticipant.lifespan - (player.gameParticipant.participant.lifespan)}/${player.gameParticipant.lifespan}"))
            }
        }
    }

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/records ")
    }

    override fun getUsage(ctx: CommandContext?): String? {
        if (ctx !is BukkitCommandContext) {
            return super.getUsage(ctx)
        }

        show(ctx, ctx.player)
        return ""
    }

    override fun onError(commandContext: CommandContext?, throwable: Throwable?) {}

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext, @Arg(value = "player") player: Player) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }

        show(ctx, player)
    }
}

@InjectableComponent
@Command(value = ["seppuku"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandSeppuku(val context: GameContext) : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/seppuku ")
    }

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }

        if (context.state != GameState.PreGame) {
            ctx.sendMessage(MessageType.ERROR, "You cannot use this command during the game.")
            return
        }

        RxBus.publish(CommandSeppukuEvent(ctx.player))
    }
}

@InjectableComponent
@Command(value = ["list", "l"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandList(val context: GameContext) : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/list ")
    }

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext) {
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()

        val players = context.players.joinToString { it.displayName }
        val spectators = context.spectators.joinToString { it.displayName }

        ctx.sendMessage(MessageType.INFO, Chat.message("There are &8[&6$onlinePlayers&8/&6$maxPlayers&8]&r players online"))
        ctx.sendMessage(MessageType.INFO, Chat.message("&8- &f&lIngame&8: $players"))
        ctx.sendMessage(MessageType.INFO, Chat.message("&8- &f&lWatching&8: $spectators"))
    }
}
@InjectableComponent
@Command(value = ["vote", "v"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandVote() : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/v ")
    }

    override fun getUsage(ctx: CommandContext?): String? {
        if (ctx !is BukkitCommandContext) {
            return super.getUsage(ctx)
        }

        RxBus.publish(CommandVoteEvent(ctx.player, null))
        return ""
    }

    override fun onError(commandContext: CommandContext?, throwable: Throwable?) {}

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext, @Arg("number") number: Int) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }

        RxBus.publish(CommandVoteEvent(ctx.player, number))
    }
}

@InjectableComponent
@Command(value = ["bounty"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandBounty(val context: GameContext, val bountyService: BountyService) : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/bounty ")
    }

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext,
                  @Arg(value = "player") player: Player,
                  @Arg(value = "amount") amount: Int) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }
        if (context.players.contains(ctx.player) || context.spectators.contains(player)) {
            ctx.sendMessage(MessageType.INFO, Chat.message("&cYou cannot use this command."))
            return
        }

        bountyService.addBounty(ctx.player, player, amount)
        ctx.sendMessage(MessageType.INFO, Chat.message("&7Are you sure&8? &cThis cannot be undone! &7Use &8[&6/confirmbounty&8] &7 to confirm&8."))
    }
}

@InjectableComponent
@Command(value = ["confirmbounty"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandConfirmBounty(val bountyService: BountyService) : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/confirmbounty ")
    }

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }

        if (!bountyService.contains(ctx.player)) {
            ctx.sendMessage(MessageType.ERROR, Chat.message("&cYou should use /bounty first."))
            return
        }

        bountyService.confirmBounty(ctx.player)?.let {
            Bukkit.broadcastMessage(Chat.message("&3Bounty has been set on ${it.first.displayName}&3 by ${ctx.player.displayName}&3 for &8[&e${it.second}&8] &3points&8."))
        }
    }
}

@InjectableComponent
@Command(value = ["sponsor"], permissionNode = "sg.default")
@CommandPresence(DefaultPresenceProvider::class)
class CommandSponsor(val context: GameContext) : BaseCommand() {

    override fun init() {
        super.init()
        usage = Chat.message("&cUsage&8: &c/sponsor ")
    }

    @Command("#")
    fun onCommand(ctx: BukkitCommandContext,
                  @Arg(value = "player") player: Player) {
        if (!ctx.isPlayer) {
            ctx.sendMessage(MessageType.ERROR, "Console cannot use this command.")
            return
        }

        if (context.players.contains(ctx.player)) {
            ctx.sendMessage(MessageType.ERROR, Chat.message("&cYou cannot use this command."))
            return
        }

        if (context.spectators.contains(player)) {
            ctx.sendMessage(MessageType.ERROR, Chat.message("&cSponsor can be player only."))
            return
        }

        RxBus.publish(CommandSponsorEvent(ctx.player, player))
    }
}