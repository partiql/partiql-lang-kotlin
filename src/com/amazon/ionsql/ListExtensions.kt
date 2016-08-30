/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

val <T> List<T>.head: T?
    get() = firstOrNull()

val <T> List<T>.tail: List<T>
    get() = when (size) {
        0, 1 -> emptyList()
        else -> subList(1, size)
    }

fun <T> List<T>.headTailIterator(): Iterator<Pair<T, List<T>>> = object : Iterator<Pair<T, List<T>>> {
    var curr = this@headTailIterator

    override fun hasNext(): Boolean = curr.isEmpty()

    override fun next(): Pair<T, List<T>> {
        val pair = Pair(curr.head!!, curr.tail)
        curr = curr.tail
        return pair
    }
}