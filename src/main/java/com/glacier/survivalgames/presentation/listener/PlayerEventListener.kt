package com.glacier.survivalgames.presentation.listener

import com.glacier.survivalgames.domain.model.StateMachine
import com.glacier.survivalgames.domain.model.GameState
import com.glacier.survivalgames.domain.model.state.Deathmatch
import com.glacier.survivalgames.domain.service.GameMapService
import com.glacier.survivalgames.domain.service.GameParticipantService
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.LocationUtils
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.bukkit.events.player.PlayerDamageByPlayerEvent
import io.fairyproject.bukkit.events.player.PlayerDamageEvent
import io.fairyproject.bukkit.listener.RegisterAsListener
import io.fairyproject.container.InjectableComponent
import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import io.papermc.lib.PaperLib
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

@InjectableComponent
@RegisterAsListener
class PlayerEventListener(val stateMachine: StateMachine<GameState>,
                          val participantService: GameParticipantService,
                          val gameMapService: GameMapService) : Listener {

    private val spawnLocation: Location? by lazy {
        val spawn = BukkitPlugin.INSTANCE.config.getString("settings.lobby-spawn", "")
        if (!(spawn.isNullOrEmpty())) {
            return@lazy LocationUtils.getLocationFromString(spawn)
        }
        return@lazy null
    }

    private val lastAttacked: MutableMap<Player, Player> = mutableMapOf()
    private val lastAttackedTask: MutableMap<Player, ScheduledTask<*>> = mutableMapOf()

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        e.player.gameMode = GameMode.SURVIVAL
        e.player.health = 20.0
        e.player.foodLevel = 20
        e.player.level = 0
        e.player.exp = 0.0f
        e.player.inventory.clear()
        e.player.inventory.helmet = null
        e.player.inventory.chestplate = null
        e.player.inventory.leggings = null
        e.player.inventory.boots = null

        e.player.activePotionEffects.forEach { e.player.removePotionEffect(it.type) }

        when (stateMachine.currentState?.key) {
            GameState.Lobby -> {
                spawnLocation?.let { PaperLib.teleportAsync(e.player, it) }

                val participant = participantService.addPlayer(e.player)
                val points = NumberFormat.getIntegerInstance(Locale.US).format(participant.points)
                e.joinMessage = Chat.message("&8[&e$points&8]${e.player.displayName} &6has joined&8.", prefix = false)
            }
            else -> {
                e.player.allowFlight = true

                Bukkit.getOnlinePlayers().forEach { it.hidePlayer(e.player) }
//                Bukkit.getWorld(gameMapService.playingGameMap().worldName)?.let {
//                    PaperLib.teleportAsync(e.player, it.spawnLocation)
//                }

                val it = participantService.addSpectator(e.player)
                val points = NumberFormat.getIntegerInstance(Locale.US).format(it.points)
                e.joinMessage = Chat.message("&8[&e$points&8]${e.player.displayName} &6has joined&8.", prefix = false)
            }
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage = null
        participantService.remove(e.player)?.let {
            participantService.save(it)
            val points = NumberFormat.getIntegerInstance(Locale.US).format(it.points)
            e.quitMessage = Chat.message("&8[&e$points&8]${e.player.displayName} &6has left&8.", prefix = false)
        }
    }

    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        participantService.get(e.player)?.let {
            val name = it.player.displayName
            val message = e.message
            val points = NumberFormat.getIntegerInstance(Locale.US).format(it.points)

            e.format = Chat.message("&8[&e$points&8]$name&8: &f$message", prefix = false)
            when (stateMachine.currentState?.key) {
                GameState.Lobby -> {}
                GameState.EndGame, GameState.Cleanup -> {
                    if (participantService.spectators().containsKey(e.player.uniqueId))
                        e.format = Chat.message("&8[&e$points&8]&4SPEC&8:$name&8: &f$message", prefix = false)
                }
                else -> {
                    // ゲーム中の場合、観戦者のチャットはプレイヤーに表示させない
                    if (participantService.spectators().containsKey(e.player.uniqueId)) {

                        e.format = Chat.message("&8[&e$points&8]&4SPEC&8:$name&8: &f$message", prefix = false)
                        e.recipients.removeAll(participantService.players()
                            .mapNotNull { p -> p.value.player }
                            .toSet())
                    }
                    else {
                        e.format = Chat.message("&8[&a${it.bounties}&8]&c${it.position}&8:$name&8: &f$message", prefix = false)
                    }
                }
            }

        }
    }

    @EventHandler
    fun onFoodChange(e: FoodLevelChangeEvent) {
        when (stateMachine.currentState?.key) {
            GameState.Lobby, GameState.PreGame -> {
                e.isCancelled = true
            }
            else -> {}
        }
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        if (e.clickedBlock == null) {
            return
        }

        when (stateMachine.currentState?.key) {
            GameState.Lobby -> e.isCancelled = true
            else -> {
                if (participantService.spectators().containsKey(e.player.uniqueId)) {
                    val random = participantService.players().values.map { it.player }.random()

                    PaperLib.teleportAsync(e.player, random.location)
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onEnterVehicle(e: VehicleEnterEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        if (e.entered is Player && participantService.spectators().containsKey(e.entered.uniqueId)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        when (stateMachine.currentState?.key) {
            GameState.Lobby -> e.isCancelled = true
            else -> {
                if (participantService.spectators().containsKey(e.player.uniqueId)
                    || !breakableBlock(e.block.type)) {
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        if (e.block.type != Material.FIRE) {
            e.isCancelled = true
            return
        }

        val max = Material.FLINT_AND_STEEL.maxDurability
        val item = e.player.inventory.itemInHand
        item.durability = item.durability.plus(max.div(4)).toShort()

        if (max <= item.durability) {
            e.player.inventory.remove(item)
            e.player.playSound(e.player.location, Sound.ITEM_BREAK, 1.0f, 1.0f)
        }
    }

    @EventHandler
    fun onInventoryOpen(e: InventoryOpenEvent) {
        if (!participantService.players().containsKey(e.player.uniqueId)) {
            return
        }

        participantService.get(e.player as Player)?.let { it.chests++ }
    }

    @EventHandler
    fun onItemPick(e: PlayerPickupItemEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }
        when (stateMachine.currentState?.key) {
            GameState.Lobby -> e.isCancelled = true
            else -> {
                if (participantService.spectators().containsKey(e.player.uniqueId)) {
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onDamage(e: PlayerDamageEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }

        when (stateMachine.currentState?.key) {
            GameState.LiveGame, GameState.Deathmatch -> {
                if (participantService.spectators().containsKey(e.player.uniqueId)) {
                    e.isCancelled = true
                    return
                }

                if (e.player.health - e.finalDamage <= 0.1) {
                    onDeathByPlayer(e.player, lastAttacked[e.player])
                }
            }
            else -> e.isCancelled = true
        }
    }

    @EventHandler
    fun onDamageByPlayer(e: PlayerDamageByPlayerEvent) {
        if (BukkitPlugin.INSTANCE.config.getBoolean("settings.maintenance-mode", false)) {
            return
        }

        when (stateMachine.currentState?.key) {
            GameState.LiveGame, GameState.Deathmatch -> {
                if (participantService.spectators().containsKey(e.player.uniqueId)
                    || participantService.spectators().containsKey(e.damager.uniqueId)) {
                    e.isCancelled = true
                    return
                }

                lastAttacked[e.player] = e.damager
                if (lastAttackedTask.containsKey(e.player)) {
                    val task = lastAttackedTask.remove(e.player)
                    task?.cancel()
                }

                lastAttackedTask[e.player] = MCSchedulers.getGlobalScheduler().schedule({ lastAttacked.remove(e.player) }, 1)

                if (e.player.health - e.finalDamage <= 0.1) {
                    onDeathByPlayer(e.player, e.damager)
                    if (participantService.players().size <= 1) {
                        stateMachine.sendEvent(GameState.EndGame)
                    }
                }
            }
            else -> e.isCancelled = true
        }
    }


    private fun onDeathByPlayer(player: Player, damager: Player? = null) {
        val location = player.location
        val world = location.world

        world.strikeLightningEffect(location)

        participantService.remove(player)
        val victim = participantService.addSpectator(player)
        player.sendMessage(Chat.message("&aYou have been eliminated from the games."))

        if (stateMachine.currentState?.key == GameState.LiveGame) {
            Bukkit.broadcastMessage(Chat.message("&aOnly &8[&6${participantService.players().size}&8]&a tributes remain!"))
            Bukkit.broadcastMessage(Chat.message("&aThere are &8[&6${participantService.spectators().size}&8]&a spectators watching the game."))
        }
        Bukkit.broadcastMessage(Chat.message("&6A cannon has be heard in the distance&8.", prefix = false))

        // 倒された人はポイントをドロップする
        val drop = max(victim.points / 20, 5)
        victim.points = max(victim.points - drop, 0)
        participantService.save(victim)
        player.sendMessage(Chat.message("&3You've lost &8[&e$drop&8]&3 points for dying&8!"))

        // 倒した人はポイントをゲットする
        participantService.get(damager)?.let {
            it.points += drop
            it.kills++
        }
        damager?.sendMessage(Chat.message("&3You've gained &8[&e$drop&8]&3 points for killing ${player.displayName}&8!"))

        if (victim.bounties > 0) {
            val bounties = victim.bounties
            participantService.get(damager)?.let {
                it.points += victim.bounties
                victim.bounties = 0
            }

            damager?.sendMessage(Chat.message("&3You've gained &8[&e$bounties&8]&3 extra points from bounties set on ${player.displayName}&8."))
            Bukkit.broadcastMessage(Chat.message("&6A bounty of &8[$bounties&8] points has been claimed upon ${player.displayName}'s death&8.", prefix = false))
        }
    }

    private fun breakableBlock(type: Material): Boolean
        = type == Material.GRASS ||
        type == Material.LONG_GRASS ||

        type == Material.LEAVES ||
        type == Material.LEAVES_2 ||
        type == Material.FIRE ||
        type == Material.VINE
}