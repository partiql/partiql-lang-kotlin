/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** Simple source line/column value. */
data class SourcePosition(val line: Long, val column: Long) {
    override fun toString(): String = "line $line, column $column"
}
