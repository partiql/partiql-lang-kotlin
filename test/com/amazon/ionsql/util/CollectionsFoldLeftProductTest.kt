/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.util

import java.util.*

class CollectionsFoldLeftProductTest : CollectionsProductTest() {
    var expectedCount: Int = 0
    var actualCount: Int = 0

    data class Context(val level: Int, val id: Int)

    override fun <T> product(collections: List<List<T>>): Iterable<List<T>> {
        expectedCount = collections.dropLast(1).fold(listOf(1)) { prev, list ->
            prev + (prev.last() * list.size)
        }.fold(0) { prev, count ->
            prev + count
        }

        actualCount = 0

        val contexts = HashSet<Context>()

        // unique ID per context to make sure we getting this passed down
        var id = 0
        return collections.foldLeftProduct(Context(0, 0)) { ctx, list ->
            actualCount++
            assertEquals(collections.indexOfFirst { it === list }, ctx.level)
            assertFalse("$ctx already in $contexts", ctx in contexts)
            contexts += ctx
            list.asSequence().mapIndexed { idx, elem ->
                Pair(Context(ctx.level + 1, id++), elem)
            }.iterator()
        }
    }

    override fun afterProduct() {
        assertEquals(expectedCount, actualCount)
    }
}
