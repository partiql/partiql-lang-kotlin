package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*

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
class SubstringExprFunction(ion: IonSystem): NullPropagatingExprFunction("substring", (2..3), ion) {
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
            args[1].type != ExprValueType.INT                  -> errNoContext("Argument 2 of substring was not INT.",
                                                                               internal = false)
            args.size > 2 && args[2].type != ExprValueType.INT -> errNoContext("Argument 3 of substring was not INT.",
                                                                               internal = false)
        }
    }
}