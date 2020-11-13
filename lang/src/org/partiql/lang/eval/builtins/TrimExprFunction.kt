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

package org.partiql.lang.eval.builtins

import com.amazon.ion.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.builtins.TrimSpecification.*
import org.partiql.lang.util.*

/**
 * From section 6.7 of SQL 92 spec:
 * ```
 * 6) If <trim function> is specified, then
 *   a) If FROM is specified, then either <trim specification> or <trim character> or both shall be specified.
 *
 *   b) If <trim specification> is not specified, then BOTH is implicit.
 *
 *   c) If <trim character> is not specified, then ' ' is implicit.
 *
 *   d) If TRIM ( SRC ) is specified, then TRIM ( BOTH ' ' FROM SRC ) is implicit.
 *
 *   e) The data type of the <trim function> is variable-length character string with maximum length equal to the
 *   fixed length or maximum variable length of the <trim source>.
 *
 *   f) If a <trim character> is specified, then <trim character> and <trim source> shall be comparable.
 *
 *   g) The character repertoire and form-of-use of the <trim function> are the same as those of the <trim source>.
 *
 *   h) The collating sequence and the coercibility attribute are determined as specified for monadic operators in
 *      Subclause 4.2.3, "Rules determining collating sequence usage", where the <trim source> of TRIM plays the
 *      role of the monadic operand.
 *  ```
 *
 *  Where:
 *  * `<trim specification> ::= LEADING | TRAILING | BOTH`
 *  * `<trim character> ::= <character value expression>`
 *  * `<trim source> ::= <character value expression>`
 */
internal class TrimExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("trim", 1..3, valueFactory) {
    private val DEFAULT_TO_REMOVE = " ".codePoints().toArray()
    private val DEFAULT_SPECIFICATION = BOTH

    private fun IntArray.leadingTrimOffset(toRemove: IntArray): Int {
        var offset = 0

        while (offset < this.size && toRemove.contains(this[offset])) offset += 1

        return offset
    }

    private fun IntArray.trailingTrimOffSet(toRemove: IntArray): Int {
        var offset = 0

        while (offset < this.size && toRemove.contains(this[size - offset - 1])) offset += 1

        return offset
    }

    private fun IntArray.leadingTrim(toRemove: IntArray): String {
        val offset = this.leadingTrimOffset(toRemove)

        return String(this, offset, this.size - offset)
    }

    private fun IntArray.trailingTrim(toRemove: IntArray) = String(this, 0, this.size - this.trailingTrimOffSet(toRemove))

    private fun IntArray.trim(toRemove: IntArray): String {
        val leadingOffset = this.leadingTrimOffset(toRemove)
        val trailingOffset = this.trailingTrimOffSet(toRemove)
        val length = Math.max(0, this.size - trailingOffset - leadingOffset)

        return String(this, leadingOffset, length)
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val (type, toRemove, string) = extractArguments(args)

        return when (type) {
            BOTH, NONE -> valueFactory.newString(string.trim(toRemove))
            LEADING    -> valueFactory.newString(string.leadingTrim(toRemove))
            TRAILING   -> valueFactory.newString(string.trailingTrim(toRemove))
        }
    }

    private fun ExprValue.codePoints() = this.stringValue().codePoints().toArray()

    private fun extractArguments(args: List<ExprValue>): Triple<TrimSpecification, IntArray, IntArray> {
        return when (args.size) {
            1    -> Triple(DEFAULT_SPECIFICATION, DEFAULT_TO_REMOVE, args[0].codePoints())
            2    -> {

                if(!args[0].type.isText){
                    errNoContext("with two arguments trim's first argument must be either the " +
                                 "specification or a 'to remove' string",
                                 internal = false)
                }

                val specification = TrimSpecification.from(args[0])
                val toRemove = when(specification) {
                    NONE -> args[0].codePoints()
                    else -> DEFAULT_TO_REMOVE
                }

                Triple(specification, toRemove, args[1].codePoints())
            }
            3    -> {
                val specification = TrimSpecification.from(args[0])
                if(specification == NONE) {
                    errNoContext("'${args[0].stringValue()}' is an unknown trim specification, " +
                                 "valid vales: ${TrimSpecification.validValues}",
                                 internal = false)
                }


                Triple(specification, args[1].codePoints(), args[2].codePoints())
            }

            // arity is checked by NullPropagatingExprFunction
            else -> errNoContext("invalid trim arguments, should be unreachable", internal = true)
        }
    }
}

private enum class TrimSpecification {
    BOTH, LEADING, TRAILING, NONE;

    companion object {
        fun from(arg: ExprValue) = when (arg.stringValue()) {
            "both"     -> BOTH
            "leading"  -> LEADING
            "trailing" -> TRAILING
            else       -> NONE
        }

        val validValues = TrimSpecification.values()
            .filter { it == NONE }
            .joinToString()
    }

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}
