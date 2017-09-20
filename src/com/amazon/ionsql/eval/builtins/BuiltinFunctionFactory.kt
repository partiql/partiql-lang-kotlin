package com.amazon.ionsql.eval.builtins

import com.amazon.ion.IonSystem
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.eval.ExprFunction.Companion.over
import com.amazon.ionsql.util.*

class BuiltinFunctionFactory(private val ion: IonSystem) {


    fun createFunctionMap() : Map<String, ExprFunction> =  mapOf(
            "upper" to this.upper(),
            "lower" to this.lower(),
            "exists" to this.exists(),
            "substring" to this.substring(),
            "char_length" to this.char_length(),
            "character_length" to this.char_length()
    )

    fun exists(): ExprFunction =
            over { _, args ->
                when (args.size) {
                    1 -> {
                        args[0].asSequence().any().exprValue(ion)
                    }
                    else -> err("Expected a single argument for exists but found: ${args.size}")
                }
            }

    /*
        From the SQL-92 spec, page 135:
         1) If <character substring function> is specified, then:

            a) Let C be the value of the <character value expression>,
               let LC be the length of C, and
               let S be the value of the <start position>.

            b) If <string length> is specified, then:
                let L be the value of <string length> and
                let E be S+L.
                Otherwise:
                    let E be the larger of LC + 1 and S.

            c) If either C, S, or L is the null value, then the result of
              the <character substring function> is the null value.

            d) If E is less than S, then an exception condition is raised:
              data exception-substring error.

            e) Case:

              i) If S is greater than LC or if E is less than 1, then the
                 result of the <character substring function> is a zero-
                 length string.

             ii) Otherwise,

                 1) Let S1 be the larger of S and 1. Let E1 be the smaller
                   of E and LC+1. Let L1 be E1-S1.

                 2) The result of the <character substring function> is
                   a character string containing the L1 characters of C
                   starting at character number S1 in the same order that
                   the characters appear in C.

        Pseudocode:
            func substring():
                # Section 1-a
                C = <string to be sliced>
                LC = LENGTH(C)
                S = <start position>

                # Section 1-b
                L = <length of slice, optional>
                if L is specified:
                    E = S + L
                else:
                    E = greater_of(LC + 1, S)

                # Section 1-c:
                if C, S, or L is null:
                    return null

                # Section 1-d
                if E < S:
                    throw exception

                # Section 1-e-i
                if S > LC or E < 1:
                    return ''
                else:
                    # Section 1-e-ii
                    S1 = greater_of(S, 1)
                    E1 = lesser_of(E, LC + 1)
                    L1 = E1 - S1
                    return java's substring(C, S1, E1)
     */
    fun substring(): ExprFunction = object : ExprFunction {
        override fun call(env: Environment, args: List<ExprValue>): ExprValue {
            validateArugments(args)

            when {
                args.isAnyMissing() -> return missingExprValue(ion)
                args.isAnyNull() -> return nullExprValue(ion)
            }

            val str = args[0].stringValue()
            val codePointCount = str.codePointCount(0, str.length)

            var startPosition = args[1].numberValue().toInt()
            var endPosition = if (args.count() == 2)
                codePointCount
            else
                startPosition + args[2].numberValue().toInt() - 1

            //Clamp start and end indexes to values that won't make java's substring barf
            startPosition = if (startPosition < 1) 1 else startPosition
            endPosition = if (endPosition >= codePointCount) codePointCount else endPosition

            if (endPosition < startPosition)
                err("Invalid start position or length arguments to substring function.")

            val byteIndexStart = str.offsetByCodePoints(0, startPosition - 1)
            val byteIndexEnd = str.offsetByCodePoints(0, endPosition)

            return str.substring(byteIndexStart, byteIndexEnd).exprValue(ion)
        }


        private fun validateArugments(args: List<ExprValue>) {
            when {
                args.count() != 2 && args.count() != 3 ->
                    err("Expected 2 or 3 arguments for substring instead of ${args.size}")
                !args[1].ionValue.isNullValue && !args[1].ionValue.isNumeric ->
                    err("Argument 2 of substring was not numeric")
                args.count() > 2 && !args[2].ionValue.isNullValue && !args[2].ionValue.isNumeric ->
                    err("Argument 3 of substring was not numeric")
            }
        }
    }

    fun char_length(): ExprFunction = object : OneArgExprFunction() {
        override val functionName: String = "char_length"

        override fun call(arg: ExprValue): ExprValue {
            val s:String = arg.stringValue()
            return s.codePointCount(0, s.length).exprValue(ion)
        }
    }

    fun upper(): ExprFunction = object : OneArgExprFunction() {
        override val functionName: String = "upper"

        override fun call(arg: ExprValue): ExprValue {
            return arg.stringValue().toUpperCase().exprValue(ion)
        }
    }

    fun lower(): ExprFunction = object : OneArgExprFunction() {
        override val functionName: String = "lower"

        override fun call(arg: ExprValue): ExprValue {
            return arg.stringValue().toLowerCase().exprValue(ion)
        }
    }

    /**
     * This base class can be used by simple functions taking only a single argument.
     * Provides default behaviors:
     *  - Validates that only one argument has been passed.
     *  - If that argument is null, returns null.
     *  - If that argument is missing, returns missing.
     */
    private abstract inner class OneArgExprFunction : ExprFunction {
        abstract val functionName: String

        abstract fun call(arg: ExprValue): ExprValue

        override fun call(env: Environment, args: List<ExprValue>): ExprValue {
            validateArguments(args)
            when {
                args.isAnyNull() -> return nullExprValue(ion)
                args.isAnyMissing() -> return missingExprValue(ion)
            }

            return call(args[0])
        }

        protected fun validateArguments(args: List<ExprValue>) {
            when {
                args.count() != 1 -> err("Expected 1 argument for ${functionName} instead of ${args.size}")
            }
        }
    }
}


