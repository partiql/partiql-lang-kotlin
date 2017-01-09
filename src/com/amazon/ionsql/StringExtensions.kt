/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** Provides a lazy sequence over the code points in the given string. */
fun String.codePointSequence(): Sequence<Int> {
    var text = this
    return Sequence {
        var pos = 0
        object : Iterator<Int> {
            override fun hasNext(): Boolean = pos < text.length
            override fun next(): Int {
                val cp = text.codePointAt(pos)
                pos += Character.charCount(cp)
                return cp
            }
        }
    }
}
