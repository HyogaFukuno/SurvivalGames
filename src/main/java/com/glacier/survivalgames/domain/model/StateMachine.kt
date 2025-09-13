package com.glacier.survivalgames.domain.model

import io.fairyproject.container.InjectableComponent
import io.fairyproject.log.Log
import io.fairyproject.scheduler.response.TaskResponse
import java.util.concurrent.CompletableFuture
import kotlin.collections.get

@InjectableComponent
class StateMachine<TKey> {

    private enum class TransitionPhase {
        NONE,
        EXIT,
        ENTER
    }

    var currentState: State<TKey>? = null
    private val states = mutableMapOf<TKey, State<TKey>>()
    private var nextKey: TKey? = null
    private var nowTransition = false
    private var transitionPhase = TransitionPhase.NONE

    fun registerState(state: State<TKey>) {
        registerState(state.key, state)
    }

    fun registerState(key: TKey, state: State<TKey>) {
        states[key] = state
    }

    fun setStartState(key: TKey) { sendEvent(key) }

    fun sendEvent(key: TKey) { nextKey = key }

    fun update(): TaskResponse<Boolean> = when {
        nextKey != null && !nowTransition -> {
            nowTransition = true
            transitionPhase = TransitionPhase.EXIT
            transition()
        }
        nowTransition -> transition()
        else -> currentState?.update() ?: TaskResponse.continueTask()
    }

    private fun transition(): TaskResponse<Boolean> = when (transitionPhase) {
        TransitionPhase.EXIT -> {
            val result = currentState?.exit() ?: TaskResponse.success(true)
            if (result?.result == true) {
                currentState = states[nextKey]
                transitionPhase = TransitionPhase.ENTER
                nextKey = null
                currentState?.enter()
            }
            TaskResponse.continueTask()
        }
        TransitionPhase.ENTER -> {
            val result = currentState?.enter()
            if (result?.result == true) {
                nowTransition = false
                transitionPhase = TransitionPhase.NONE
                currentState?.update()
                Log.info("Successfully transitioned.")
            }
            TaskResponse.continueTask()
        }
        else -> TaskResponse.continueTask()
    }

    abstract class State<TKey>(protected val stateMachine: StateMachine<TKey>, val key: TKey) {
        var remainTime = 0

        private val enterAsync: CompletableFuture<Void> by lazy { enterAsync() }
        private val exitAsync: CompletableFuture<Void> by lazy { exitAsync() }

        internal fun enter(): TaskResponse<Boolean> {
            val future = enterAsync
            return when {
                future.isDone -> TaskResponse.success(true)
                else -> TaskResponse.continueTask()
            }
        }

        internal fun exit(): TaskResponse<Boolean> {
            val future = exitAsync
            return when {
                future.isDone -> TaskResponse.success(true)
                else -> TaskResponse.continueTask()
            }
        }

        protected abstract fun enterAsync(): CompletableFuture<Void>
        internal abstract fun update(): TaskResponse<Boolean>
        internal abstract fun exitAsync(): CompletableFuture<Void>
        protected abstract fun broadcast()
        protected abstract fun shouldBroadcast(): Boolean
    }
}