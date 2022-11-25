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

import org.junit.Test
import org.partiql.lang.TestBase

open class CollectionsProductTest() : TestBase() {
    fun <T> assertCartesianProduct(collections: List<List<T>>, expected: List<List<T>>) {
        assertEquals(expected, product(collections).toList())
        afterProduct()
    }

    open fun <T> product(collections: List<List<T>>): Iterable<List<T>> = collections.product()

    open fun afterProduct() {}

    @Test
    fun empty() = assertCartesianProduct(
        listOf(listOf(1, 2, 3), listOf()),
        listOf()
    )

    @Test
    fun single() = assertCartesianProduct(
        listOf(listOf(1, 2, 3)),
        listOf(listOf(1), listOf(2), listOf(3))
    )

    @Test
    fun double() = assertCartesianProduct(
        listOf(listOf(1, 2, 3), listOf(4, 5)),
        listOf(listOf(1, 4), listOf(1, 5), listOf(2, 4), listOf(2, 5), listOf(3, 4), listOf(3, 5))
    )

    @Test
    fun triple() = assertCartesianProduct(
        listOf(listOf(1, 2, 3), listOf(4, 5), listOf(8, 9)),
        listOf(
            listOf(1, 4, 8), listOf(1, 4, 9),
            listOf(1, 5, 8), listOf(1, 5, 9),

            listOf(2, 4, 8), listOf(2, 4, 9),
            listOf(2, 5, 8), listOf(2, 5, 9),

            listOf(3, 4, 8), listOf(3, 4, 9),
            listOf(3, 5, 8), listOf(3, 5, 9)
        )
    )

    @Test
    fun quadruple() = assertCartesianProduct(
        listOf(listOf(1, 2, 3), listOf(4, 5), listOf(8, 9), listOf(0)),
        listOf(
            listOf(1, 4, 8, 0), listOf(1, 4, 9, 0),
            listOf(1, 5, 8, 0), listOf(1, 5, 9, 0),

            listOf(2, 4, 8, 0), listOf(2, 4, 9, 0),
            listOf(2, 5, 8, 0), listOf(2, 5, 9, 0),

            listOf(3, 4, 8, 0), listOf(3, 4, 9, 0),
            listOf(3, 5, 8, 0), listOf(3, 5, 9, 0)
        )
    )
}
