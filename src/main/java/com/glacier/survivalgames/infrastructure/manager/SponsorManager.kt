package com.glacier.survivalgames.infrastructure.manager

import com.glacier.survivalgames.domain.extension.gameParticipant
import com.glacier.survivalgames.domain.model.event.CommandSponsorEvent
import com.glacier.survivalgames.domain.model.event.SponsorEvent
import com.glacier.survivalgames.infrastructure.view.SponsorView
import com.glacier.survivalgames.utils.Chat
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@InjectableComponent
class SponsorManager(val viewManager: ViewManager) {

    private val sponsors = mutableMapOf<Player, Player>()
    private val disposable = CompositeDisposable()

    @PreInitialize
    fun onPreInitialize() {
        RxBus.listen<CommandSponsorEvent>().subscribe(this::onOpenSponsorMenu).addTo(disposable)
        RxBus.listen<SponsorEvent>().subscribe(this::onSuccessSponsor).addTo(disposable)
    }

    @PreDestroy
    fun onPreDestroy() { disposable.clear() }

    private fun onOpenSponsorMenu(e: CommandSponsorEvent) {
        sponsors[e.player] = e.target
        viewManager.open<SponsorView>(e.player)
    }

    private fun onSuccessSponsor(e: SponsorEvent) {
        sponsors.remove(e.player)?.let {
            if (e.player.gameParticipant.points < e.price) {
                e.player.sendMessage(Chat.message("&cYou do not have enough points."))
                return
            }

            e.player.gameParticipant.points -= e.price
            it.inventory.addItem(ItemStack(e.type))
            it.playSound(it.location, Sound.LEVEL_UP, 1.0f, 1.0f)

            e.player.sendMessage(Chat.message("Sponsor item sent."))
            it.sendMessage(Chat.message("Surprise - you were sponsored by ${e.player.displayName}&r!"))
            it.sendMessage(Chat.message("Remember to thank them!"))
        }
    }
}