package org.partiql.lang.util

import java.lang.IllegalStateException

/**
 * Converts [this] [Long] to a [Int], throwing an exception in case the value is outside the range of an [Int].
 *
 * This is needed because Kotlin's default [Long.toInt()] returns `-1` instead of throwing an exception.
 */
fun Long.toIntExact():Int =
    if(this !in Int.MIN_VALUE..Int.MAX_VALUE) {
        throw IllegalStateException("Long value is not within Int.MIN_VALUE..Int.MAX_VALUE")
    } else {
        this.toInt()
    }