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
