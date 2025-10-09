package com.glacier.survivalgames.presentation.view

import com.glacier.survivalgames.AudienceProvider
import com.glacier.survivalgames.application.service.GameMapService
import com.glacier.survivalgames.domain.message.OpenServerManagementMessage
import com.glacier.survivalgames.utils.RxBus
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreDestroy
import io.fairyproject.container.PreInitialize
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import me.devnatan.inventoryframework.ViewFrame

@InjectableComponent
class ViewAdapter(mapService: GameMapService, audienceProvider: AudienceProvider) {

    private val frame by lazy { ViewFrame.create(BukkitPlugin.INSTANCE)
        .with(ServerManagement.MainView())
        .with(ServerManagement.MapSelectionView(mapService, audienceProvider))
        .with(ServerManagement.PrivacyView())
        .with(ServerManagement.MutatorsView())
    }
    private val disposable = CompositeDisposable()

    @PreInitialize
    fun onPreInitialize() {
        RxBus.listen<OpenServerManagementMessage>().subscribe { frame.open(ServerManagement.MainView::class.java, it.player) }.addTo(disposable)

        frame.register()
    }

    @PreDestroy
    fun onPreDestroy() {
        disposable.clear()
        frame.unregister()
    }
}