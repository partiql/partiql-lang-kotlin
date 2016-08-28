/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

val <T> List<T>.head: T
    get() = first()

val <T> List<T>.tail: List<T>
    get() = when (size) {
        0, 1 -> emptyList()
        else -> subList(1, size)
    }