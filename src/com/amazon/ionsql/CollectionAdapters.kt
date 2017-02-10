/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import java.util.*
import java.util.Collections.*

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

/**
 * Calculates the cartesian product of the given ordered lists of collections
 * of homogeneous values.
 *
 * Note that the requirement of the underlying [Iterable] is that it is repeatable,
 * though for singleton cases, this requirement is relaxed.
 */
fun <T> List<Iterable<T?>>.product(): Iterable<List<T?>> = object : Iterable<List<T?>> {
    override fun iterator(): Iterator<List<T?>> {
        val collections = this@product

        // special case for singleton
        if (collections.size == 1) {
            val iterator = collections[0].iterator()
            return object : Iterator<List<T?>> {
                override fun hasNext() = iterator.hasNext()
                override fun next(): List<T?> = singletonList(iterator.next())
            }
        }

        if (collections.any { !it.iterator().hasNext() }) {
            // one of the collections is empty, the cross product is empty
            return emptyList<List<T?>>().iterator()
        }

        val iterators: MutableList<Iterator<T?>> =
            collections.mapTo(ArrayList()) { emptyList<T?>().iterator() }
        iterators[0] = collections[0].iterator()

        val curr = collections.mapTo(ArrayList<T?>()) { null }

        return object : Iterator<List<T?>> {
            override fun hasNext(): Boolean = iterators.any { it.hasNext() }

            override fun next(): List<T?> {
                // find the least significant iterator with something
                var idx = iterators.size - 1
                while (idx >= -1) {
                    val iter = iterators[idx]
                    if (iter.hasNext()) {
                        break
                    }
                    idx--
                }
                if (idx == -1) {
                    throw NoSuchElementException("Exhausted iterator")
                }

                curr[idx] = iterators[idx].next()
                idx++
                while (idx < iterators.size) {
                    val iter = collections[idx].iterator()
                    iterators[idx] = iter
                    curr[idx] = iter.next()
                    idx++
                }

                return curr.toList()
            }
        }
    }
}
