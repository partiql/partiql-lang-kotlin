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
            list.asSequence().map {
                Pair(Context(ctx.level + 1, id++), it)
            }.iterator()
        }
    }

    override fun afterProduct() {
        assertEquals(expectedCount, actualCount)
    }
}
