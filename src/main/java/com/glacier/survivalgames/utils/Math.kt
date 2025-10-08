package com.glacier.survivalgames.utils

import kotlin.math.max

fun max(a: Int?, b: Int?) = max(a ?: 0, b ?: 0)
fun max(a: Double?, b: Double?) = max(a ?: 0.0, b ?: 0.0)

fun min(a: Int?, b: Int?) = max(a ?: 0, b ?: 0)
fun min(a: Double?, b: Double?) = max(a ?: 0.0, b ?: 0.0)