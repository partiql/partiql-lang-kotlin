/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** Compiles query expression source into an [Expression]. */
interface Compiler {
    fun compile(source: String): Expression
}