package com.glacier.survivalgames.extension

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.pointer.Pointer

fun <T> Audience.getNullable(pointer: Pointer<T>): T? {
    return get(pointer).orElse(null)
}