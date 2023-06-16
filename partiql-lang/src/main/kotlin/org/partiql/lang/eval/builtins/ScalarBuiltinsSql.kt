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
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.builtins.internal.ExprFunctionBinaryNumeric
import org.partiql.lang.eval.builtins.internal.ExprFunctionMeasure
import org.partiql.lang.eval.builtins.internal.ExprFunctionUnaryNumeric
import org.partiql.lang.eval.builtins.internal.codepointLeadingTrim
import org.partiql.lang.eval.builtins.internal.codepointOverlay
import org.partiql.lang.eval.builtins.internal.codepointPosition
import org.partiql.lang.eval.builtins.internal.codepointTrailingTrim
import org.partiql.lang.eval.builtins.internal.codepointTrim
import org.partiql.lang.eval.builtins.internal.extractedValue
import org.partiql.lang.eval.builtins.internal.transformIntType
import org.partiql.lang.eval.bytesValue
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.coerceNumbers
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.exp
import org.partiql.lang.util.isNaN
import org.partiql.lang.util.isNegInf
import org.partiql.lang.util.isPosInf
import org.partiql.lang.util.ln
import org.partiql.lang.util.power
import org.partiql.lang.util.squareRoot
import org.partiql.types.AnyOfType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.unionOf
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/**
 * Reference SQL-99 20.70
 *
 * TODO replace this internal value once we have function libraries
 */
internal val SCALAR_BUILTINS_SQL = listOf(
    ExprFunctionAbs,
    ExprFunctionMod,
    ExprFunctionCeil,
    ExprFunctionCeiling,
    ExprFunctionFloor,
    ExprFunctionSqrt,
    ExprFunctionExp,
    ExprFunctionPow,
    ExprFunctionLn,
    ExprFunctionLower,
    ExprFunctionUpper,
    ExprFunctionBitLength,
    ExprFunctionCharLength,
    ExprFunctionCharacterLength,
    ExprFunctionOctetLength,
    ExprFunctionSubstring,
    ExprFunctionSubstring2,
    ExprFunctionTrim,
    ExprFunctionTrim2,
    ExprFunctionTrim3,
    ExprFunctionPosition,
    ExprFunctionOverlay,
    ExprFunctionOverlay2,
    ExprFunctionExtract,
    ExprFunctionCardinality,
    ExprFunctionPower,
    ExprFunctionPower2,
    ExprFunctionPower3
)

/**
 * ABS operates on a numeric argument and returns its absolute value in the same most specific type.
 */
internal object ExprFunctionAbs : ExprFunctionUnaryNumeric("abs") {

    override fun call(x: Number): Number = when (x) {
        is Long -> {
            if (x == Long.MIN_VALUE) {
                errIntOverflow(8)
            } else {
                kotlin.math.abs(x)
            }
        }
        is Double -> kotlin.math.abs(x)
        is Float -> kotlin.math.abs(x)
        is BigDecimal -> x.abs()
        else -> errNoContext(
            message = "Unknown number type",
            errorCode = ErrorCode.INTERNAL_ERROR,
            internal = true
        )
    }
}

/**
 * MOD operates on two exact numeric arguments with scale 0 (zero) and returns
 * the modulus (remainder) of the first argument divided by the second argument as an exact
 * numeric with scale 0 (zero).
 *
 * If the second argument is zero, an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown.
 */
internal object ExprFunctionMod : ExprFunction {

    override val signature = FunctionSignature(
        name = "mod",
        requiredParameters = listOf(StaticType.INT, StaticType.INT),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val x = required[0].intValue()
        val y = required[1].intValue()
        if (y == 0) {
            errNoContext(
                message = "Division by zero",
                errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                internal = true
            )
        }
        val result = x % y
        return ExprValue.newInt(result)
    }
}

/**
 * Returns the nearest integer greater than or equal to the input.
 */
internal object ExprFunctionCeil : ExprFunctionUnaryNumeric("ceil") {

    override fun call(x: Number): Number = when (x) {
        java.lang.Double.POSITIVE_INFINITY, java.lang.Double.NEGATIVE_INFINITY, java.lang.Double.NaN -> x
        // support for numbers that are larger than 64 bits.
        else -> transformIntType(bigDecimalOf(x).setScale(0, RoundingMode.CEILING).toBigIntegerExact())
    }
}

/**
 * Returns the nearest integer greater than or equal to the input.
 */
internal object ExprFunctionCeiling : ExprFunctionUnaryNumeric("ceiling") {

    override fun call(x: Number): Number = ExprFunctionCeil.call(x)
}

/**
 * Returns the absolute value of the given number.
 * Note that abs(n) will throw an EVALUATOR_INTEGER_OVERFLOW when n is both of type INT and n = INT.MIN_VALUE.
 */
internal object ExprFunctionFloor : ExprFunctionUnaryNumeric("floor") {

    override fun call(x: Number): Number = when (x) {
        java.lang.Double.POSITIVE_INFINITY, java.lang.Double.NEGATIVE_INFINITY, java.lang.Double.NaN -> x
        else -> transformIntType(bigDecimalOf(x).setScale(0, RoundingMode.FLOOR).toBigIntegerExact())
    }
}

/**
 * Returns the square root of the given number.
 * The input number is required to be non-negative.
 */
internal object ExprFunctionSqrt : ExprFunctionUnaryNumeric("sqrt") {

    override fun call(x: Number): Number {
        if (x < 0L) {
            errNoContext(
                "Cannot take root of a negative number",
                errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                internal = false
            )
        }
        return when (x) {
            is Long -> kotlin.math.sqrt(x.toDouble())
            is Double -> kotlin.math.sqrt(x)
            is Float -> kotlin.math.sqrt(x)
            is BigDecimal -> x.squareRoot()
            else -> errNoContext(
                message = "Unknown number type",
                errorCode = ErrorCode.INTERNAL_ERROR,
                internal = true
            )
        }
    }
}

/**
 * Returns e^x for a given x.
 *
 * - exp(NaN) is NaN
 * - exp(+Inf) is +Inf
 * - exp(-Inf) is 0.0
 */
internal object ExprFunctionExp : ExprFunctionUnaryNumeric("exp") {
    override fun call(x: Number): Number = when (x) {
        is Long -> kotlin.math.exp(x.toDouble())
        is Double -> kotlin.math.exp(x)
        is Float -> kotlin.math.exp(x)
        is BigDecimal -> x.exp()
        else -> errNoContext(
            message = "Unknown number type",
            errorCode = ErrorCode.INTERNAL_ERROR,
            internal = true
        )
    }
}

/**
 * Coercion is needed for this operation, since it is binary.
 * if the operation involves special value `+inf`, `-inf`, `nan`, the result will be a float.
 * else if the operation involves decimal, the result will be a decimal
 * else the result will be a float.
 *
 * Note that if x is a negative number, than y must be an integer value, (not necessarily integer type),
 * otherwise an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown.
 * Special Case:
 * pow(x, 0.0) is 1.0;
 * pow(x, 1.0) == x;
 * pow(x, NaN) is NaN;
 * pow(NaN, x) is NaN for x != 0.0;
 * pow(x, Inf) is NaN for abs(x) == 1.0
 */
internal object ExprFunctionPow : ExprFunctionBinaryNumeric("pow") {

    override fun call(x: Number, y: Number): Number {
        // CoerceNumber(double, bigDecimal) will attempt to convert the double value to bigDecimal
        // and in case of the double value being one of the special number, `+inf`, `-inf`, `nan`,
        // an error will be thrown.
        // we (presumably) want to avoid this
        val (first, second) = if (x.isPosInf || x.isNegInf || x.isNaN) {
            x to y.toDouble()
        } else if (y.isPosInf || y.isNegInf || y.isNaN) {
            x.toDouble() to y
        } else {
            coerceNumbers(x, y)
        }

        return when (first) {
            is Long -> first.toDouble().pow(second.toDouble())
            is Double -> {
                if (first < 0.0 && ((second as Double) % 1.0 != 0.0)) {
                    errNoContext(
                        message = "a negative number raised to a non-integer power yields a complex result",
                        errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                        internal = false
                    )
                }
                first.pow(second as Double)
            }
            is BigDecimal ->
                try {
                    first.power(second as BigDecimal)
                } catch (e: Exception) {
                    errNoContext(
                        message = e.message ?: "Arithmetic Error",
                        errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                        internal = false
                    )
                }
            else -> throw IllegalStateException()
        }
    }
}

/**
 * Returns the natural log of the given number.
 *
 * The input number is required to be a positive number, otherwise an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown.
 */
internal object ExprFunctionLn : ExprFunctionUnaryNumeric("ln") {

    override fun call(x: Number): Number {
        if (x <= 0L) {
            errNoContext(
                "Cannot take root of a non-positive number",
                errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                internal = false
            )
        }
        return when (x) {
            is Long -> kotlin.math.ln(x.toDouble())
            is Double -> kotlin.math.ln(x)
            is Float -> kotlin.math.ln(x)
            is BigDecimal -> x.ln()
            else -> errNoContext(
                message = "Unknown number type",
                errorCode = ErrorCode.INTERNAL_ERROR,
                internal = true
            )
        }
    }
}

/**
 * Given a string convert all upper case characters to lower case characters.
 *
 * Any non-upper cased characters remain unchanged. This operation does rely on the locale specified by the runtime
 * configuration. This implementation uses Java's String.toLowerCase().
 */
internal object ExprFunctionLower : ExprFunction {

    override val signature = FunctionSignature(
        name = "lower",
        requiredParameters = listOf(StaticType.TEXT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val str = required[0].stringValue()
        val result = str.toLowerCase()
        return ExprValue.newString(result)
    }
}

/**
 * Given a string convert all lower case characters to upper case characters.
 *
 * Any non-lower cases characters remain unchanged. This operation does rely on the locale specified by the runtime
 * configuration. The implementation uses Java's String.toLowerCase().
 */
internal object ExprFunctionUpper : ExprFunction {

    override val signature = FunctionSignature(
        name = "upper",
        requiredParameters = listOf(AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val str = required[0].stringValue()
        val result = str.toUpperCase()
        return ExprValue.newString(result)
    }
}

/**
 * This function is to test overloading.
 */
internal object ExprFunctionPower : ExprFunction {

    override val signature = FunctionSignature(
        name = "query_power",
        requiredParameters = listOf(StaticType.INT, StaticType.INT),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val base = required[0].numberValue()
        val exponent = required[1].numberValue()
        val result = Math.pow(base.toDouble(), exponent.toDouble()).toInt()
        return ExprValue.newInt(result)
    }
}

internal object ExprFunctionPower2 : ExprFunction {

    override val signature = FunctionSignature(
        name = "query_power",
        requiredParameters = listOf(StaticType.INT),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val base = required[0].numberValue()
        val result = Math.pow(base.toDouble(), 2.0).toInt()
        return ExprValue.newInt(result)
    }
}

internal object ExprFunctionPower3 : ExprFunction {

    override val signature = FunctionSignature(
        name = "query_power",
        requiredParameters = listOf(StaticType.INT, StaticType.BOOL),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val base = required[0].numberValue()
        val square = required[1].booleanValue()
        var result = 0
        if (square) {
            result = Math.pow(base.toDouble(), 5.0).toInt()
        } else {
            result = Math.pow(base.toDouble(), 4.0).toInt()
        }
        return ExprValue.newInt(result)
    }
}

/**
 * Returns the number of bits in the input string
 */
internal object ExprFunctionBitLength : ExprFunctionMeasure("bit_length", BITSTRING) {

    override fun call(value: ExprValue): Int = ExprFunctionOctetLength.call(value) * 8
}

/**
 * Counts the number of characters in the specified string, where 'character' is defined as a single unicode code point.
 *
 * Same as CHARACTER_LENGTH
 */
internal object ExprFunctionCharLength : ExprFunctionMeasure("char_length", StaticType.TEXT) {

    override fun call(value: ExprValue): Int = codepointLength(value)
}

/**
 * Counts the number of characters in the specified string, where 'character' is defined as a single unicode code point.
 *
 * Same as CHAR_LENGTH
 */
internal object ExprFunctionCharacterLength : ExprFunctionMeasure("character_length", StaticType.TEXT) {

    override fun call(value: ExprValue): Int = codepointLength(value)
}

private fun codepointLength(value: ExprValue): Int {
    val str = value.stringValue()
    return str.codePointCount(0, str.length)
}

/**
 * If an <octet length expression> is specified, then let S be the <string value expression>. The
 * result of the <octet length expression> is the smallest integer not less than the quotient of the
 * division (BIT_LENGTH(S)/8).
 */
internal object ExprFunctionOctetLength : ExprFunctionMeasure("octet_length", BITSTRING) {

    override fun call(value: ExprValue): Int {
        val bytes = when {
            value.type.isText -> value.stringValue().toByteArray(Charsets.UTF_8)
            else -> {
                // Does not throw if value.type.isLob, otherwise will throw the appropriate evaluation exception
                value.bytesValue()
            }
        }
        return bytes.size
    }
}

/**
 * Built in function to return the substring of an existing string. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * From the SQL-92 spec, page 135:
 * ```
 * 1) If <character substring function> is specified, then:
 *      a) Let C be the value of the <character value expression>,
 *      let LC be the length of C, and
 *      let S be the value of the <start position>.
 *
 *      b) If <string length> is specified, then:
 *      let L be the value of <string length> and
 *      let E be S+L.
 *      Otherwise:
 *          let E be the larger of LC + 1 and S.
 *
 *      c) If either C, S, or L is the null value, then the result of
 *      the <character substring function> is the null value.
 *
 *      d) If E is less than S, then an exception condition is raised:
 *      data exception-substring error.
 *
 *      e) Case:
 *          i) If S is greater than LC or if E is less than 1, then the
 *          result of the <character substring function> is a zero-
 *          length string.
 *
 *          ii) Otherwise,
 *              1) Let S1 be the larger of S and 1. Let E1 be the smaller
 *              of E and LC+1. Let L1 be E1-S1.
 *
 *              2) The result of the <character substring function> is
 *              a character string containing the L1 characters of C
 *              starting at character number S1 in the same order that
 *              the characters appear in C.
 *
 * Pseudocode:
 *      func substring():
 *          # Section 1-a
 *          str = <string to be sliced>
 *          strLength = LENGTH(str)
 *          startPos = <start position>
 *
 *          # Section 1-b
 *          sliceLength = <length of slice, optional>
 *          if sliceLength is specified:
 *              endPos = startPos + sliceLength
 *          else:
 *              endPos = greater_of(strLength + 1, startPos)
 *
 *          # Section 1-c:
 *          if str, startPos, or (sliceLength is specified and is null):
 *              return null
 *
 *          # Section 1-d
 *          if endPos < startPos:
 *              throw exception
 *
 *          # Section 1-e-i
 *          if startPos > strLength or endPos < 1:
 *              return ''
 *          else:
 *              # Section 1-e-ii
 *              S1 = greater_of(startPos, 1)
 *              E1 = lesser_of(endPos, strLength + 1)
 *              L1 = E1 - S1
 *              return java's substring(C, S1, E1)
 */

internal object ExprFunctionSubstring : ExprFunction {

    /**
     * TODO implement substring pattern (STRING, STRING, INT) -> STRING, requires sql regex pattern parsing
     */
    override val signature = FunctionSignature(
        name = "substring",
        requiredParameters = listOf(StaticType.STRING, StaticType.INT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val target = required[0].stringValue()
        if (required[1].type != ExprValueType.INT) {
            errNoContext(
                message = "Function substring with two parameters must be of form substring(<string> FROM <int>)",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }
        val startPosition = required[1].intValue()
        return substring(target, startPosition)
    }

    private fun substring(target: String, startPosition: Int, quantity: Int? = null): ExprValue {
        val codePointCount = target.codePointCount(0, target.length)
        if (startPosition > codePointCount) {
            return ExprValue.newString("")
        }

        // startPosition starts at 1
        // calculate this before adjusting start position to account for negative startPosition
        val endPosition = when (quantity) {
            null -> codePointCount
            else -> Integer.min(codePointCount, startPosition + quantity - 1)
        }

        // Clamp start indexes to values that make sense for java substring
        val adjustedStartPosition = Integer.max(0, startPosition - 1)

        if (endPosition < adjustedStartPosition) {
            return ExprValue.newString("")
        }

        val byteIndexStart = target.offsetByCodePoints(0, adjustedStartPosition)
        val byteIndexEnd = target.offsetByCodePoints(0, endPosition)

        return ExprValue.newString(target.substring(byteIndexStart, byteIndexEnd))
    }
}

internal object ExprFunctionSubstring2 : ExprFunction {

    /**
     * TODO implement substring pattern (STRING, STRING, INT) -> STRING, requires sql regex pattern parsing
     */
    override val signature = FunctionSignature(
        name = "substring",
        requiredParameters = listOf(StaticType.STRING, StaticType.INT, StaticType.INT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val quantity = required.last().intValue()
        if (quantity < 0) {
            errNoContext(
                message = "Argument 3 of substring has to be greater than 0.",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }
        val target = required[0].stringValue()
        if (required[1].type != ExprValueType.INT) {
            errNoContext(
                message = "Regular expression substring (SQL T581) currently not supported",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }
        val startPosition = required[1].intValue()
        return substring(target, startPosition, quantity)
    }

    private fun substring(target: String, startPosition: Int, quantity: Int? = null): ExprValue {
        val codePointCount = target.codePointCount(0, target.length)
        if (startPosition > codePointCount) {
            return ExprValue.newString("")
        }

        // startPosition starts at 1
        // calculate this before adjusting start position to account for negative startPosition
        val endPosition = when (quantity) {
            null -> codePointCount
            else -> Integer.min(codePointCount, startPosition + quantity - 1)
        }

        // Clamp start indexes to values that make sense for java substring
        val adjustedStartPosition = Integer.max(0, startPosition - 1)

        if (endPosition < adjustedStartPosition) {
            return ExprValue.newString("")
        }

        val byteIndexStart = target.offsetByCodePoints(0, adjustedStartPosition)
        val byteIndexEnd = target.offsetByCodePoints(0, endPosition)

        return ExprValue.newString(target.substring(byteIndexStart, byteIndexEnd))
    }
}
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
internal object ExprFunctionTrim : ExprFunction {

    override val signature = FunctionSignature(
        name = "trim",
        requiredParameters = listOf(StaticType.TEXT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val result = trim1Arg(required[0])
        return ExprValue.newString(result)
    }

    private fun trim1Arg(sourceString: ExprValue): String = codepointTrim(sourceString.stringValue())
}

internal object ExprFunctionTrim2 : ExprFunction {

    override val signature = FunctionSignature(
        name = "trim",
        requiredParameters = listOf(StaticType.TEXT, StaticType.STRING),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(
        session: EvaluationSession,
        required: List<ExprValue>,
    ): ExprValue {
        val result = trim2Arg(required[0], required[1])
        return ExprValue.newString(result)
    }

    /**
     * Small optimization to eliminate the TrimSpecification enum, still temporary since we'll add function lowering.
     * Return the behavior on switch rather than switch to get an enum then switch again on the enum for behavior.
     */
    private fun getTrimFnOrNull(trimSpecification: String): ((String, String?) -> String)? =
        when (trimSpecification.toLowerCase().trim()) {
            "both" -> ::codepointTrim
            "leading" -> ::codepointLeadingTrim
            "trailing" -> ::codepointTrailingTrim
            else -> null
        }

    private fun trim2Arg(specificationOrToRemove: ExprValue, sourceString: ExprValue): String {
        // Type signature checking should have handled this
        if (!specificationOrToRemove.type.isText) {
            errNoContext(
                message = "with two arguments trim's first argument must be either the specification or a 'to remove' string",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                internal = false
            )
        }
        val arg0 = specificationOrToRemove.stringValue()
        val arg1 = sourceString.stringValue()
        return when (val trimFn = getTrimFnOrNull(arg0)) {
            null -> codepointTrim(arg1, arg0)
            else -> trimFn.invoke(arg1, null)
        }
    }
}

internal object ExprFunctionTrim3 : ExprFunction {

    override val signature = FunctionSignature(
        name = "trim",
        requiredParameters = listOf(StaticType.TEXT, StaticType.STRING, StaticType.STRING),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(
        session: EvaluationSession,
        required: List<ExprValue>,
    ): ExprValue {
        val result = trim3Arg(required[0], required[1], required[2])
        return ExprValue.newString(result)
    }

    /**
     * Small optimization to eliminate the TrimSpecification enum, still temporary since we'll add function lowering.
     * Return the behavior on switch rather than switch to get an enum then switch again on the enum for behavior.
     */
    private fun getTrimFnOrNull(trimSpecification: String): ((String, String?) -> String)? =
        when (trimSpecification.toLowerCase().trim()) {
            "both" -> ::codepointTrim
            "leading" -> ::codepointLeadingTrim
            "trailing" -> ::codepointTrailingTrim
            else -> null
        }

    private fun trim3Arg(specification: ExprValue, toRemove: ExprValue, sourceString: ExprValue): String {
        val arg0 = specification.stringValue()
        val arg1 = toRemove.stringValue()
        val arg2 = sourceString.stringValue()
        return when (val trimFn = getTrimFnOrNull(arg0)) {
            null -> {
                // TODO with ANTLR, the invalid_argument should be caught in visitTrimFunction in PartiQLVisitor
                // We should decide where this error shall be caught and whether it is a parsing error or an evaluator error.
                // This error should also be caught in the function lowering as part of logical planning
                errNoContext(
                    message = "'$arg0' is an unknown trim specification, valid values: BOTH, TRAILING, LEADING",
                    errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                    internal = false
                )
            }
            else -> trimFn.invoke(arg2, arg1)
        }
    }
}

/**
 * SQL-99 p.15 and p.21
 *
 * <position expression> determines the first position, if any, at which one string, S1, occurs within
 * another, S2. If S1 is of length zero, then it occurs at position 1 (one) for any value of S2. If S1
 * does not occur in S2, then zero is returned. The declared type of a <position expression> is exact numeric
 *
 * <position expression> when applied to binary strings is identical in syntax and semantics to the
 * corresponding operation on character strings except that the operands are binary strings.
 */
internal object ExprFunctionPosition : ExprFunction {

    override val signature = FunctionSignature(
        name = "position",
        requiredParameters = listOf(StaticType.TEXT, StaticType.TEXT),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        // POSITION(s1 IN s2)
        val s1 = required[0].stringValue()
        val s2 = required[1].stringValue()
        val result = codepointPosition(s2, s1)
        return ExprValue.newInt(result)
    }
}

/**
 * <character overlay function> ::=
 *      OVERLAY <left paren> <character value expression>
 *          PLACING <character value expression>
 *          FROM <start position>
 *          [ FOR <string length> ]
 *     <right paren>
 *
 * <character overlay function> is a function, OVERLAY, that modifies a string argument by replacing
 * a given substring of the string, which is specified by a given numeric starting position and a
 * given numeric length, with another string (called the replacement string). When the length of
 * the substring is zero, nothing is removed from the original string and the string returned by the
 * function is the result of inserting the replacement string into the original string at the starting position.
 *
 * The <character overlay function> is equivalent to:
 *
 *   SUBSTRING ( CV FROM 1 FOR SP - 1 ) || RS || SUBSTRING ( CV FROM SP + SL )
 *
 * Where CV is the characters value, RS is the replacement string, SP is start position, SL is CV length
 */
internal object ExprFunctionOverlay : ExprFunction {

    override val signature = FunctionSignature(
        name = "overlay",
        requiredParameters = listOf(StaticType.TEXT, StaticType.TEXT, StaticType.INT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return overlay(required[0], required[1], required[2])
    }

    private fun overlay(arg0: ExprValue, arg1: ExprValue, arg2: ExprValue, arg3: ExprValue? = null): ExprValue {
        val position = arg2.intValue()
        if (position < 1) {
            errNoContext(
                message = "invalid position '$position', must be at least 1",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                internal = false
            )
        }
        val source = arg0.stringValue()
        val overlay = arg1.stringValue()
        val length = arg3?.intValue() ?: overlay.length
        val result = codepointOverlay(source, overlay, position, length)
        return ExprValue.newString(result)
    }
}

internal object ExprFunctionOverlay2 : ExprFunction {

    override val signature = FunctionSignature(
        name = "overlay",
        requiredParameters = listOf(StaticType.TEXT, StaticType.TEXT, StaticType.INT, StaticType.INT),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return overlay(required[0], required[1], required[2], required[3])
    }

    private fun overlay(arg0: ExprValue, arg1: ExprValue, arg2: ExprValue, arg3: ExprValue? = null): ExprValue {
        val position = arg2.intValue()
        if (position < 1) {
            errNoContext(
                message = "invalid position '$position', must be at least 1",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_TRIM,
                internal = false
            )
        }
        val source = arg0.stringValue()
        val overlay = arg1.stringValue()
        val length = arg3?.intValue() ?: overlay.length
        val result = codepointOverlay(source, overlay, position, length)
        return ExprValue.newString(result)
    }
}

/**
 * Given a datetime part and a datetime type returns then datetime's datetime part value.
 *
 * ExtractDateTimePart is one of
 * * year
 * * month
 * * day
 * * hour
 * * minute
 * * second
 * * timezone_hour
 * * timezone_minute
 *
 * DateTime type is one of
 * * DATE
 * * TIME
 * * TIMESTAMP
 *
 * Note that ExtractDateTimePart differs from DateTimePart in DATE_ADD.
 *
 * SQL Note:
 * Header : EXTRACT(edp FROM t)
 * Purpose : Given a datetime part, edp, and a datetime type t return t's value for edp. This function allows for t to
 * be unknown (null or missing) but not edp. If t is unknown the function returns null.
 */
internal object ExprFunctionExtract : ExprFunction {

    private val DATETIME = unionOf(StaticType.TIMESTAMP, StaticType.TIME, StaticType.DATE)

    override val signature = FunctionSignature(
        name = "extract",
        requiredParameters = listOf(StaticType.SYMBOL, DATETIME),
        returnType = StaticType.DECIMAL
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return when {
            required[1].isUnknown() -> ExprValue.nullValue
            else -> eval(required)
        }
    }

    private fun eval(args: List<ExprValue>): ExprValue {
        val dateTimePart = args[0].dateTimePartValue()
        val extractedValue = when (args[1].type) {
            ExprValueType.TIMESTAMP -> args[1].timestampValue().extractedValue(dateTimePart)
            ExprValueType.DATE -> args[1].dateValue().extractedValue(dateTimePart)
            ExprValueType.TIME -> args[1].timeValue().extractedValue(dateTimePart)
            else -> errNoContext(
                "Expected date, time or timestamp: ${args[1]}",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }

        return ExprValue.newDecimal(extractedValue)
    }
}

/**
 * Builtin function to return the size of a container type, i.e. size of Lists, Structs and Bags. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * syntax: `size(<container>)` where container can be a BAG, SEXP, STRUCT or LIST.
 */
internal object ExprFunctionCardinality : ExprFunction {

    override val signature = FunctionSignature(
        name = "cardinality",
        requiredParameters = listOf(unionOf(StaticType.LIST, StaticType.BAG, StaticType.STRUCT, StaticType.SEXP)),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val collection = required[0]
        val result = collection.count()
        return ExprValue.newInt(result)
    }
}
