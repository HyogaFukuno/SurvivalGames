package com.glacier.survivalgames.utils

import kotlin.random.Random

class WeightedList<T> {

    private val items = mutableListOf<WeightedItem<T>>()
    private var totalWeight = 0.0

    private data class WeightedItem<T>(val item: T, val weight: Double)

    fun add(item: T, weight: Double = 1.0) {
        require(weight > 0) { "重みは0より大きい値である必要があります" }
        items.add(WeightedItem(item, weight))
        totalWeight += weight
    }

    fun addAll(items: Collection<T>, weight: Double = 1.0) {
        items.forEach { add(it, weight) }
    }

    fun addAll(items: Collection<Pair<T, Double>>) {
        items.forEach { add(it.first, it.second) }
    }

    fun random(): T {
        if (items.isEmpty()) {
            throw IllegalStateException("items is empty.")
        }

        val randomValue = Random.nextDouble(totalWeight)
        var currentWeight = 0.0

        for (weightedItem in items) {
            currentWeight += weightedItem.weight
            if (randomValue < currentWeight) {
                return weightedItem.item
            }
        }

        return items.last().item
    }

    fun random(count: Int): List<T> {
        require(count >= 0) { "count must be greater than or equal to 0" }
        return (0 until count).map { random() }
    }

    fun randomUnique(count: Int): List<T> {
        require(count >= 0) { "count must be greater than or equal to 0" }
        require(count <= items.size) { "count must be less than or equal to items size" }

        if (count == 0) return emptyList()

        val selectedItems = mutableSetOf<T>()
        val remainingItems = items.toMutableList()
        var remainingWeight = totalWeight

        while (selectedItems.size < count && remainingItems.isNotEmpty()) {
            val randomValue = Random.nextDouble(remainingWeight)
            var currentWeight = 0.0

            val iterator = remainingItems.iterator()
            while (iterator.hasNext()) {
                val weightedItem = iterator.next()
                currentWeight += weightedItem.weight
                if (randomValue <= currentWeight) {
                    selectedItems.add(weightedItem.item)
                    remainingWeight -= weightedItem.weight
                    iterator.remove()
                    break
                }
            }
        }

        return selectedItems.toList()
    }
}