/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Test

class CollectionsTest : Base() {
    fun <T> assertCartesianProduct(collections: List<List<T>>, expected: List<List<T>>) {
        assertEquals(expected, collections.product().toList())
    }

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