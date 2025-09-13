package com.glacier.survivalgames.infrastructure.skedule

import io.fairyproject.mc.scheduler.MCSchedulers
import io.fairyproject.scheduler.ScheduledTask
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.isActive
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
class BukkitDispatcher(val async: Boolean = false) : CoroutineDispatcher(), Delay {

    private val runTaskLater: (Runnable, Long) -> ScheduledTask<*> =
        if (async)
            MCSchedulers.getAsyncScheduler()::schedule
        else
            MCSchedulers.getGlobalScheduler()::schedule
    private val runTask: (Runnable) -> ScheduledTask<*> =
        if (async)
            MCSchedulers.getAsyncScheduler()::schedule
        else
            MCSchedulers.getGlobalScheduler()::schedule

    @ExperimentalCoroutinesApi
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val task = runTaskLater(
            Runnable {
                continuation.apply { resumeUndispatched(Unit) }
            },
            timeMillis / 50)
        continuation.invokeOnCancellation { task.cancel() }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!context.isActive) {
            return
        }

        if (!async && Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            runTask(block)
        }
    }

}