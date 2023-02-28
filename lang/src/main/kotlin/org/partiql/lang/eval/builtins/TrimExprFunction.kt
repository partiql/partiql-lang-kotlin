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

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.builtins.TrimSpecification.BOTH
import org.partiql.lang.eval.builtins.TrimSpecification.LEADING
import org.partiql.lang.eval.builtins.TrimSpecification.NONE
import org.partiql.lang.eval.builtins.TrimSpecification.TRAILING
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.VarargFormalParameter
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StaticType.Companion.unionOf

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
internal class TrimExprFunction : ExprFunction {
    override val signature =
        FunctionSignature(
            name = "trim",
            requiredParameters = listOf(unionOf(StaticType.STRING, StaticType.SYMBOL)),
            variadicParameter = VarargFormalParameter(StaticType.STRING, 0..2),
            returnType = StaticType.STRING
        )

    private val DEFAULT_TO_REMOVE = " ".codePoints().toArray()
    private val DEFAULT_SPECIFICATION = TrimSpecification.BOTH

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

    private fun ExprValue.codePoints() = this.stringValue().codePoints().toArray()

    private fun trim(type: TrimSpecification, toRemove: IntArray, sourceString: IntArray): ExprValue {
        return when (type) {
            BOTH, NONE -> ExprValue.newString(sourceString.trim(toRemove))
            LEADING -> ExprValue.newString(sourceString.leadingTrim(toRemove))
            TRAILING -> ExprValue.newString(sourceString.trailingTrim(toRemove))
        }
    }

    private fun trim1Arg(sourceString: ExprValue) = trim(DEFAULT_SPECIFICATION, DEFAULT_TO_REMOVE, sourceString.codePoints())

    private fun trim2Arg(specificationOrToRemove: ExprValue, sourceString: ExprValue): ExprValue {
        if (!specificationOrToRemove.type.isText) {
            errNoContext(
                "with two arguments trim's first argument must be either the " +
                    "specification or a 'to remove' string",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                internal = false
            )
        }

        val trimSpec = TrimSpecification.from(specificationOrToRemove)
        val toRemove = when (trimSpec) {
            NONE -> specificationOrToRemove.codePoints()
            else -> DEFAULT_TO_REMOVE
        }

        return trim(trimSpec, toRemove, sourceString.codePoints())
    }
    private fun trim3Arg(specification: ExprValue, toRemove: ExprValue, sourceString: ExprValue): ExprValue {
        val trimSpec = TrimSpecification.from(specification)
        if (trimSpec == NONE) {
            // todo with ANTLR, the invalid_argument should be caught in visitTrimFunction in PartiQLVisitor
            // we should decide where this error shall be caught and whether it is a parsing error or an evaluator error.
            errNoContext(
                "'${specification.stringValue()}' is an unknown trim specification, " +
                    "valid values: ${TrimSpecification.validValues}",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                internal = false
            )
        }

        return trim(trimSpec, toRemove.codePoints(), sourceString.codePoints())
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>) = trim1Arg(required[0])
    override fun callWithVariadic(session: EvaluationSession, required: List<ExprValue>, variadic: List<ExprValue>): ExprValue {
        return when (variadic.size) {
            0 -> trim1Arg(required[0])
            1 -> trim2Arg(required[0], variadic[0])
            2 -> trim3Arg(required[0], variadic[0], variadic[1])
            else -> errNoContext("invalid trim arguments, should be unreachable", errorCode = ErrorCode.INTERNAL_ERROR, internal = true)
        }
    }
}

private enum class TrimSpecification {
    BOTH, LEADING, TRAILING, NONE;

    companion object {
        fun from(arg: ExprValue) = when (arg.stringValue().toLowerCase()) {
            "both" -> BOTH
            "leading" -> LEADING
            "trailing" -> TRAILING
            else -> NONE
        }

        val validValues = TrimSpecification.values()
            .filterNot { it == NONE }
            .joinToString()
    }

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}
