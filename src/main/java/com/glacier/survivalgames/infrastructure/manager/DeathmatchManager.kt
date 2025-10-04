package com.glacier.survivalgames.infrastructure.manager

import com.glacier.survivalgames.domain.model.event.CommandSponsorEvent
import com.glacier.survivalgames.domain.model.event.OpenDeathmatchMenuEvent
import com.glacier.survivalgames.domain.model.event.SponsorEvent
import com.glacier.survivalgames.infrastructure.view.DeathmatchView
import com.glacier.survivalgames.infrastructure.view.SponsorView
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

@InjectableComponent
class DeathmatchManager(val viewManager: ViewManager) {

    private val disposable = CompositeDisposable()

    @PreInitialize
    fun onPreInitialize() {
        RxBus.listen<OpenDeathmatchMenuEvent>().subscribe(this::onOpenDeathmatchMenu).addTo(disposable)
    }

    @PreDestroy
    fun onPreDestroy() { disposable.clear() }

    private fun onOpenDeathmatchMenu(e: OpenDeathmatchMenuEvent) {
        viewManager.open<DeathmatchView>(e.player)
    }
}