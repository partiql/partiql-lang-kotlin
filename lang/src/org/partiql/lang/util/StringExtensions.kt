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

/** Provides a lazy sequence over the code points in the given string. */
fun String.codePointSequence(): Sequence<Int> {
    val text = this
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

/**
 * Truncates [String] to given utf8 byte length.
 */
fun String.truncateToUtf8ByteLength(byteLength: Int): String {
    var byteCount = 0
    var i = 0
    var charCount = i
    while (byteCount < byteLength && i < this.length) {
        val c = this[i]
        val cValue = c.toInt()
        if (cValue < 0x80) {
            byteCount++
        } else if (cValue < 0x800) {
            byteCount += 2
        } else if (c.isHighSurrogate()) {
            // count lowSurrogate as well and jump to next character.
            byteCount += 4
            i += 1
        } else {
            byteCount += 3
        }
        i += 1
        if (byteCount <= byteLength) {
            charCount = i
        }
    }

    return this.substring(0 until charCount)
}

/**
 * Returns utf8 byte length of the string.
 */
val String.utf8ByteLength: Int
    get() {
        var count = 0
        var skipLowSurrogate = false
        for (c in this) {
            if (skipLowSurrogate) {
                count += 2
                skipLowSurrogate = false
            } else {
                val cValue = c.toInt()
                if (cValue < 0x80) {
                    count++
                } else if (cValue < 0x800) {
                    count += 2
                } else if (c.isHighSurrogate()) {
                    count += 2
                    skipLowSurrogate = true
                } else {
                    count += 3
                }
            }
        }
        return count
    }
