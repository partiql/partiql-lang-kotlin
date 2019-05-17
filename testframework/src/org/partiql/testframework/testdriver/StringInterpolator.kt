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

package org.partiql.testframework.testdriver

import com.amazon.ion.*
import org.partiql.lang.util.*

class UndefinedVariableInterpolationException(val variableName: String)
     : Exception("Undefined interpolation variable: ${variableName}")

fun String.interpolate(struct: IonStruct, ions: IonSystem): String {
    //This regex should be:  \$\{\s*(\S*)\s*\}
    //(thanks Kotlin for making already difficult to read regexes even worse!)
    val regex = Regex("\\${'$'}\\{\\s*(\\S*?)\\s*\\}")

    val matches = regex.findAll(this)
    //val results = matches.toList()
    var lastOffset = 0
    val buffer = StringBuffer()
    for (result in matches) {
        val beforeVariableReference = this.substring(lastOffset, result.range.start)
        buffer.append(beforeVariableReference)

        val identifier = result.groups[1]!!.value
        val value = struct[identifier] ?: throw UndefinedVariableInterpolationException(identifier)

        if(!value.isNullValue) {
            val replaceValue: String = when (value) {
                is IonText                            -> value.stringValue()!!
                is IonInt, is IonFloat, is IonDecimal -> value.numberValue().toString()
                is IonBool                            -> if (value.booleanValue()) "true" else "false"
                is IonTimestamp                       -> value.toString()
                is IonContainer                       -> {
                    val sb = StringBuilder()
                    ions.newTextWriter(sb).use { writer ->
                        ions.newReader(value).use { reader ->
                            reader.next()
                            writer.writeValue(reader)
                        }
                    }
                    sb.toString()
                }
                else                                  -> {
                    throw IllegalArgumentException("I don't know how to convert ${value.type} to a string.")
                }
            }
            buffer.append(replaceValue)
        }

        lastOffset = result.range.endInclusive + 1
    }

    val trailingText = this.substring(lastOffset)
    buffer.append(trailingText)
    return buffer.toString()
}
