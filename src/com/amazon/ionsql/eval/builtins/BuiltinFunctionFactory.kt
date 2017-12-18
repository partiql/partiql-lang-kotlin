package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*

internal class BuiltinFunctionFactory(private val ion: IonSystem) {

    fun createFunctionMap(): Map<String, ExprFunction> = mapOf("upper" to this.upper(),
                                                               "lower" to this.lower(),
                                                               "date_add" to DateAddExprFunction(ion),
                                                               "date_diff" to DateDiffExprFunction(ion),
                                                               "exists" to this.exists(),
                                                               "extract" to ExtractExprFunction(ion),
                                                               "substring" to this.substring(),
                                                               "char_length" to this.charLength(),
                                                               "character_length" to this.charLength(),
                                                               "trim" to TrimExprFunction(ion),
                                                               "to_string" to ToStringExprFunction(ion),
                                                               "to_timestamp" to ToTimestampExprFunction(ion),
                                                               "utcnow" to this.utcNow())

    fun exists(): ExprFunction = ExprFunction.over { _, args ->
        when (args.size) {
            1    -> {
                args[0].asSequence().any().exprValue(ion)
            }
            else -> errNoContext("Expected a single argument for exists but found: ${args.size}", internal = false)
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
                str = <string to be sliced>
                strLength = LENGTH(str)
                startPos = <start position>

                # Section 1-b
                sliceLength = <length of slice, optional>
                if sliceLength is specified:
                    endPos = startPos + sliceLength
                else:
                    endPos = greater_of(strLength + 1, startPos)

                # Section 1-c:
                if str, startPos, or (sliceLength is specified and is null):
                    return null

                # Section 1-d
                if endPos < startPos:
                    throw exception

                # Section 1-e-i
                if startPos > strLength or endPos < 1:
                    return ''
                else:
                    # Section 1-e-ii
                    S1 = greater_of(startPos, 1)
                    E1 = lesser_of(endPos, strLength + 1)
                    L1 = E1 - S1
                    return java's substring(C, S1, E1)
     */
    fun substring(): ExprFunction = object : NullPropagatingExprFunction("substring", (2..3), ion) {
        override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
            validateArguments(args)

            val str = args[0].stringValue()
            val codePointCount = str.codePointCount(0, str.length)

            var startPosition = args[1].numberValue().toInt()
            var endPosition = if (args.count() == 2)
                codePointCount
            else
                startPosition + args[2].numberValue().toInt() - 1

            //Clamp start and end indexes to values that won't make java's substring barf
            startPosition = when {
                startPosition < 1               -> 1
                startPosition > codePointCount  -> return "".exprValue(ion)
                else -> startPosition
            }

            endPosition = if (endPosition >= codePointCount) codePointCount else endPosition

            if (endPosition < startPosition)
                errNoContext("Invalid start position or length arguments to substring function.", internal = false)

            val byteIndexStart = str.offsetByCodePoints(0, startPosition - 1)
            val byteIndexEnd = str.offsetByCodePoints(0, endPosition)

            return str.substring(byteIndexStart, byteIndexEnd).exprValue(ion)
        }

        private fun validateArguments(args: List<ExprValue>) {
            when {
                !args[1].ionValue.isNumeric ->
                    errNoContext("Argument 2 of substring was not numeric", internal = false)
                args.size > 2 && !args[2].ionValue.isNumeric ->
                    errNoContext("Argument 3 of substring was not numeric", internal = false)
            }
        }
    }

    private fun charLength(): ExprFunction = makeOneArgExprFunction("char_length") { _, arg ->
        val s = arg.stringValue()
        s.codePointCount(0, s.length).exprValue(ion)
    }

    private fun upper(): ExprFunction = makeOneArgExprFunction("upper") { _, arg ->
        arg.stringValue().toUpperCase().exprValue(ion)
    }

    private fun lower(): ExprFunction = makeOneArgExprFunction("lower") { _, arg ->
        arg.stringValue().toLowerCase().exprValue(ion)
    }

    fun utcNow(): ExprFunction = ExprFunction.over { env, args ->
        if(args.isNotEmpty()) errNoContext("utcnow() takes no arguments", internal = false)

        ion.newTimestamp(env.session.now).exprValue()
    }

    /**
     * This function can be used to create simple functions taking only a single argument with null/missing propagation
     *
     * Provides default behaviors:
     *  - Validates that only one argument has been passed.
     *  - If that argument is null, returns null.
     *  - If that argument is missing, returns missing.
     */
    private fun makeOneArgExprFunction(name: String, func: (Environment, ExprValue) -> ExprValue) =
        object : NullPropagatingExprFunction(name, 1, ion) {
            override fun eval(env: Environment, args: List<ExprValue>): ExprValue = func(env, args[0])
    }
}
