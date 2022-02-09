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

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonValue

/**
 * Formats "tagged s-expressions" in a pretty, readable fashion, i.e.
 *
 * ```
 * (tagName
 *      arg1
 *      (otherTagName
 *          arg1)
 *      arg3)
 * ```
 *
 * Don't use this in anything that expects to be able to parse the generated Ion later.
 * Seriously.  (Don't even think about it.)  This is only intended to make understanding
 * the expected/actual values easier.  Doing this in a way that generates valid Ion is
 * requires considerably more effort than this.
 */
class SexpAstPrettyPrinter(val builder: StringBuilder = StringBuilder()) {

    companion object {

        val dontIndent = setOf("*", "id", "lit")

        fun format(ionValue: IonValue): String {
            val stringBuilder = StringBuilder()
            SexpAstPrettyPrinter(stringBuilder).append(ionValue)
            return stringBuilder.toString()
        }
    }

    var nestLevel = 0

    private fun nextLine() {
        builder.appendln()
        (1..nestLevel).forEach { builder.append("    ") }
    }

    private fun append(node: IonValue) {
        when(node) {
            is IonSexp -> {
                builder.append('(')
                val tag = node.firstOrNull()
                if(tag != null) {
                    val tagSymbol = tag as? IonSymbol

                    if (tagSymbol == null) {
                        builder.append(tag.toString())
                        builder.append(" /* <-- NOTE: first position of s-exp was not a symbol */")
                    }
                    else {
                        builder.append(tagSymbol.stringValue())

                        if (dontIndent.contains(tagSymbol.stringValue())) {
                            node.drop(1).forEach {
                                builder.append(' ')
                                append(it)
                            }
                        }
                        else {
                            if (node.size > 1) {
                                nestLevel++

                                node.drop(1).forEach {
                                    nextLine()
                                    append(it)
                                }

                                nestLevel--
                            }
                        }
                    }
                }
                builder.append(')')
            }
            else -> {
                builder.append(node.toString())
            }
        }
    }

}