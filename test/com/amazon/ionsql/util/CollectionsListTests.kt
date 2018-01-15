package com.amazon.ionsql.util

import com.amazon.ionsql.*
import org.junit.Test

/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

class CollectionsListTests : Base() {

    val isEven = { x: Int -> (x % 2) == 0}
    var empty :List<Int> = listOf()
    @Test fun forAllEmptyList() = assertTrue(empty.forAll(isEven))
    @Test fun forAllTrue() = assertTrue(listOf(2, 4, 6).forAll(isEven))
    @Test fun forAllFalse() = assertFalse(listOf(2, 3, 6).forAll(isEven))
}