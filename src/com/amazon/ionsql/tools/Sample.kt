/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.tools

import com.amazon.ion.system.IonSystemBuilder

val TEXT = """(
    SELECT val..,a name AS gas, val.value AS val FROM reading.value as val
)"""

fun main(args: Array<String>) {
    val ion = IonSystemBuilder.standard().build()
    val value = ion.singleValue(TEXT)
    println(value)
}