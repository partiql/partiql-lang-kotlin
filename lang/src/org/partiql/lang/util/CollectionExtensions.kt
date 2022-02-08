/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.util

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.isUnknown

/** Returns the first element of the list or `null` if it doesn't exist. */
inline val <T> List<T>.head: T?
    get() = firstOrNull()

/** Returns the sublist not including the first element which may be the empty list. */
inline val <T> List<T>.tail: List<T>
    get() = when (size) {
        0, 1 -> emptyList()
        else -> this.subList(1, this.size)
    }


/** Returns true if any ExprValue in the Iterable is an unknown value, i.e. either MISSING or NULL. */
fun Iterable<ExprValue>.isAnyUnknown() = any { it.isUnknown() }

/** Returns true if any ExprValue in the Iterable is null. */
fun Iterable<ExprValue>.isAnyNull() = any { it.type == ExprValueType.NULL }

/** Returns true if any ExprValue in the Iterable is missing. */
fun Iterable<ExprValue>.isAnyMissing() = any { it.type == ExprValueType.MISSING }


/**
 * This should function the same as Kotlin's [Sequence<T>.take(n: Int)] function but takes
 * a long value instead.
 */
internal fun <T> Sequence<T>.take(count: Long): Sequence<T> {
    val wrappedSeq = this
    return object : Sequence<T> {
        var remaining = count

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            val wrappedIterator = wrappedSeq.iterator()

            override fun hasNext(): Boolean = wrappedIterator.hasNext() && remaining > 0

            override fun next(): T {
                val nextValue = wrappedIterator.next()
                remaining--
                return nextValue
            }
        }
    }
}

/**
 * This should function the same as Kotlin's [Sequence<T>.drop(n: Int)] function but takes
 * a long value instead.
 */
internal fun <T> Sequence<T>.drop(count: Long): Sequence<T> {
    val wrappedSeq = this
    return object : Sequence<T> {
        override fun iterator(): Iterator<T> = object : Iterator<T> {
            val wrappedIterator = wrappedSeq.iterator()
            var left = count

            private fun drop() {
                while (left > 0 && wrappedIterator.hasNext()) {
                    wrappedIterator.next()
                    left--
                }
            }

            override fun next(): T {
                drop()
                return wrappedIterator.next()
            }

            override fun hasNext(): Boolean {
                drop()
                return wrappedIterator.hasNext()
            }
        }
    }
}


/**
 * Given a predicate function, return `true` if all members of the list satisfy the predicate, return false otherwise.
 * In the case that an empty list is given, the result is `true`.
 *
 * ```
 * (a b ... z).forAll(f) <=> (f(a) && f(b) && ... && f(z))
 * ().forAll(f)          <=> true
 * ```
 *
 * @param predicate function that consumes a [T] returns a [Boolean]
 * @param T type of each element in the list
 *
 */
inline fun <T> List<T>.forAll(predicate: (T) -> Boolean) : Boolean =
    this.find { x -> !predicate(x) } == null

/**
 * Calculates the cartesian product of the given ordered lists of collections
 * of homogeneous values.
 *
 * Note that the requirement of the underlying [Iterable] is that it is repeatable,
 * though for singleton cases, this requirement is relaxed.
 */
fun <T> List<Iterable<T>>.product(): Iterable<List<T>> = foldLeftProduct(Unit) { ctx, iterable ->
    iterable.asSequence().map { Pair(ctx, it) }.iterator()
}

/**
 * Constructs a cartesian product of the given ordered list of source elements, by computing
 * the [Iterable] via a mapping function with a context.  The mapping function constructs
 * an [Iterator] of [Pair] instances of [C] and [S] that are used to derive subsequent iterators.
 *
 * @param initialContext The initial context to map/fold with.
 * @param map The mapping function to convert [S] to [Iterable] of [T] with the folded context.
 *
 * @param T The element type of the product.
 * @param S The source type of the list to be map and folded upon.
 * @param C The context to map and fold over [S].
 */
inline fun <T, S, C> List<S>.foldLeftProduct(initialContext: C,
                                      crossinline map: (C, S) -> Iterator<Pair<C, T>>) : Iterable<List<T>> =
    object : Iterable<List<T>> {
        override fun iterator(): Iterator<List<T>> {
            val sources = this@foldLeftProduct

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

/**
 * Returns the cartesian product between each list in [this] (i.e. every possible list formed by choosing an element
 * from each of the sublists where order matters). Elements in each sub-list must have homogeneous elements.
 *
 * e.g. [this] = [[A, B], [C, D], [X, Y]]
 *   => [[A, C, X], [A, C, Y], [A, D, X], [A, D, Y], [B, C, X], [B, C, Y], [B, D, X], [B, D, Y]]
 */
fun <T> List<List<T>>.cartesianProduct(): List<List<T>> {
    /**
     * Creates the cartesian product between each list in [lists] with each element in [elemsToCross]. This results in
     * a list of lists of size [lists].size * [elemsToCross].size.
     *
     * e.g. [lists] = [[A], [B], [C]], [elemsToCross] = [D, E]
     *   => [[A, D], [A, E], [B, D], [B, E], [C, D], [C, E]]
     */
    fun cartesianProductOfTwo(lists: List<List<T>>, elemsToCross: List<T>): List<List<T>> =
        lists.flatMap { list ->
            elemsToCross.map { elem ->
                list + elem
            }
        }

    return this.fold(listOf(emptyList())) { acc, argTypes ->
        cartesianProductOfTwo(acc, argTypes)
    }
}
