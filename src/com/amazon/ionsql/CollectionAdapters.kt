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
fun <T> List<Iterable<T>>.product(): Iterable<List<T>> = foldLeftProduct(Unit) { ctx, iterable ->
    iterable.asSequence().map { Pair<Unit, T>(ctx, it) }.iterator()
}

/**
 * Constructs a cartesian product of the given ordered list of source elements, by computing
 * the [Iterable] via a mapping function with a context.  The mapping function is constructs
 * an [Iterator] of [Pair] instances of [C] and [S] that are used to derive subsequent iterators.
 *
 * @param initialContext The initial context to map/fold with.
 * @param map The mapping function to convert [S] to [Iterable] of [T] with the folded context.
 *
 * @param T The element type of the product.
 * @param S The source type of the list to be map and folded upon.
 * @param C The context to map and fold over [S].
 */
fun <T, S, C> List<S>.foldLeftProduct(initialContext: C,
                                      map: (C, S) -> Iterator<Pair<C, T>>) : Iterable<List<T>> =
    object : Iterable<List<T>> {
        override fun iterator(): Iterator<List<T>> {
            val sources = this@foldLeftProduct

            // special case for singleton -- no data dependencies
            if (sources.size == 1) {
                val iterator = map(initialContext, sources[0])
                return object : Iterator<List<T>> {
                    override fun hasNext() = iterator.hasNext()
                    override fun next(): List<T> = singletonList(iterator.next().second)
                }
            }

            // seed the iterators with the highest order iterator
            val iterators: MutableList<Iterator<Pair<C, T>>> =
                sources.mapTo(ArrayList()) { emptyList<Pair<C, T>>().iterator() }
            iterators[0] = map(initialContext, sources[0])

            return object : Iterator<List<T>> {
                private var fetched = false
                private val curr = sources.mapTo(ArrayList<Pair<C, T>?>()) { null }

                fun fetchIfNeeded(): Boolean {
                    fetchLoop@ while (!fetched && iterators.any { it.hasNext() }) {
                        // start from the least significant iterator and move towards the
                        // most significant, finding the first iterator that isn't exhausted
                        var idx = iterators.size - 1
                        while (idx >= 0) {
                            val iter = iterators[idx]
                            if (iter.hasNext()) {
                                break
                            }
                            idx--
                        }

                        // at this point we are at the position of the next viable iterator
                        curr[idx] = iterators[idx].next()
                        idx++
                        while (idx < iterators.size) {
                            val ctx = curr[idx - 1]!!.first
                            // we need to materialize a new iterator
                            val iter = map(ctx, sources[idx])
                            iterators[idx] = iter
                            if (!iter.hasNext()) {
                                // an empty iterator means we have no values to fetch
                                // and we need to repeat the process
                                continue@fetchLoop
                            }
                            curr[idx] = iter.next()
                            idx++
                        }

                        // at this point we've created the row of values
                        fetched = true
                    }
                    return fetched
                }

                override fun hasNext(): Boolean = fetchIfNeeded()

                override fun next(): List<T> {
                    if (!fetchIfNeeded()) {
                        throw NoSuchElementException("Exhausted iterator")
                    }

                    fetched = false

                    // generate the record
                    return curr.map { it!!.second }
                }
            }
        }
    }
