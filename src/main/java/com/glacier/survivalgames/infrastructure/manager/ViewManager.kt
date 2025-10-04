package com.glacier.survivalgames.infrastructure.manager

import com.glacier.survivalgames.infrastructure.view.DeathmatchView
import com.glacier.survivalgames.infrastructure.view.GameMasterMainView
import com.glacier.survivalgames.infrastructure.view.SponsorView
import io.fairyproject.bootstrap.bukkit.BukkitPlugin
import io.fairyproject.container.InjectableComponent
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewFrame
import org.bukkit.entity.Player

@InjectableComponent
class ViewManager {

    private val viewFrame by lazy {
        ViewFrame.create(BukkitPlugin.INSTANCE)
            .with(SponsorView())
            .with(DeathmatchView())
            .with(GameMasterMainView())
            .register()
    }

    fun <T> open(clazz: Class<T>, player: Player) where T : View = viewFrame.open(clazz, player)

    inline fun <reified T : View> open(player: Player) = open(T::class.java, player)
}