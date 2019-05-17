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
import org.partiql.lang.util.*
import java.lang.Integer.*

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
internal class SubstringExprFunction(valueFactory: ExprValueFactory): NullPropagatingExprFunction("substring", (2..3), valueFactory) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val (target, startPosition, quantity) = extractArguments(args)

        val codePointCount = target.codePointCount(0, target.length)
        if(startPosition > codePointCount) {
            return valueFactory.newString("")
        }

        // startPosition starts at 1
        // calculate this before adjusting start position to account for negative startPosition
        val endPosition = when (quantity) {
            null -> codePointCount
            else -> min(codePointCount, startPosition + quantity - 1)
        }

        // Clamp start indexes to values that make sense for java substring
        val adjustedStartPosition =  max(0, startPosition - 1)

        if (endPosition < adjustedStartPosition) {
            return valueFactory.newString("")
        }

        val byteIndexStart = target.offsetByCodePoints(0, adjustedStartPosition)
        val byteIndexEnd = target.offsetByCodePoints(0, endPosition)

        return valueFactory.newString(target.substring(byteIndexStart, byteIndexEnd))
    }

    private fun extractArguments(args: List<ExprValue>): Triple<String, Int, Int?> {
        // type check
        when {
            args[0].type != ExprValueType.STRING                -> errNoContext("Argument 1 of substring was not STRING.",
                                                                                internal = false)
            args[1].type != ExprValueType.INT                   -> errNoContext("Argument 2 of substring was not INT.",
                                                                                internal = false)
            args.size == 3 && args[2].type != ExprValueType.INT -> errNoContext("Argument 3 of substring was not INT.",
                                                                                internal = false)
        }

        val target = args[0].stringValue()
        val startPosition = args[1].intValue()
        val quantity = when (args.size) {
            3    -> args[2].intValue()
            else -> null
        }

        if (quantity != null && quantity < 0) {
            errNoContext("Argument 3 of substring has to be greater than 0.", internal = false)
        }

        return Triple(target, startPosition, quantity)
    }
}