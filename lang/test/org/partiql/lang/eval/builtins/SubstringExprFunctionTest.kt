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

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*
import org.junit.*
import org.junit.Assert.*

class SubstringExprFunctionTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val subject = SubstringExprFunction(valueFactory)

    private val env = Environment.standard()

    private fun execSubstring(str: String, startIndex: Int): String {
        val partOfString = subject.call(env,
                                          listOf(valueFactory.newString(str),
                                                 valueFactory.newInt(startIndex)))

        return partOfString.ionValue.stringValue()!!
    }

    private fun execSubstring(str: String, startIndex: Int, length: Int): String {
        val partOfString = subject.call(env,
                                          listOf(valueFactory.newString(str),
                                                 valueFactory.newInt(startIndex),
                                                 valueFactory.newInt(length)))

        return partOfString.ionValue.stringValue()!!
    }

    @Test
    fun substringWithoutLength() {
        assertEquals("cdefg", execSubstring("abcdefg", 3))
    }

    @Test
    fun substringWithLength() {
        assertEquals("cd", execSubstring("abcdefg", 3, 2))
    }

    @Test
    fun substringWithNegativeLength() = try {
        execSubstring("abcdefg", 3, -1)
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Argument 3 of substring has to be greater than 0.", e.message)
        assertEquals(false, e.internal)
    }

    @Test
    fun substringWithNonIntSecondArgument() = try {
        subject.call(env,
                     listOf(valueFactory.newString("1234567890"),
                            valueFactory.newDecimal(3),
                            valueFactory.newInt(2)))
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Argument 2 of substring was not INT.", e.message)
        assertEquals(false, e.internal)
    }

    @Test
    fun substringWithNonIntThirdArgument() = try {
        subject.call(env,
                     listOf(valueFactory.newString("1234567890"),
                            valueFactory.newInt(3),
                            valueFactory.newDecimal(2)))
        fail("didn't throw")
    } catch (e: EvaluationException) {
        assertEquals("Argument 3 of substring was not INT.", e.message)
        assertEquals(false, e.internal)
    }

    @Test
    fun substringWithNegativeStartIndex() {
        assertEquals("abcdefg", execSubstring("abcdefg", -1))
    }

    @Test
    fun substringWithNegativeStartIndexAndLength() {
        assertEquals("ab", execSubstring("abcdefg", -1, 4))
    }

    @Test
    fun substringWithANullArgumentReturnsNull() {
        //First parameter is null
        var partOfString = subject.call(env,
                                          listOf(
                                              valueFactory.nullValue,
                                              valueFactory.newInt(3),
                                              valueFactory.newInt(2)))
        assertEquals(ExprValueType.NULL, partOfString.type)

        //Second parameter is null
        partOfString = subject.call(env,
                                      listOf(
                                          valueFactory.newString("abcdefg"),
                                          valueFactory.nullValue,
                                          valueFactory.newInt(2)))
        assertEquals(ExprValueType.NULL, partOfString.type)

        //Third parameter is null
        partOfString = subject.call(env,
                                      listOf(
                                          valueFactory.newString("abcdefg"),
                                          valueFactory.newInt(3),
                                          valueFactory.nullValue))
        assertEquals(ExprValueType.NULL, partOfString.type)
    }

    @Test
    fun emptyStringTwoArgumentsNegativeStart() {
        val actual = execSubstring("", -1)
        assertEquals("", actual)
    }

    @Test
    fun emptyStringTwoArgumentsZeroStart() {
        val actual = execSubstring("", 0)
        assertEquals("", actual)
    }

    @Test
    fun emptyStringTwoArgumentsPositiveStart() {
        val actual = execSubstring("", 99)
        assertEquals("", actual)
    }

    @Test
    fun emptyStringThreeArgumentsNegativeStart() {
        val actual = execSubstring("", -1, 999)
        assertEquals("", actual)
    }

    @Test
    fun emptyStringThreeArgumentsZeroStart() {
        val actual = execSubstring("", 0, 999)
        assertEquals("", actual)
    }

    @Test
    fun emptyStringThreeArgumentsInsufficientQuantityStart() {
        val actual = execSubstring("", -4, 1)
        assertEquals("", actual)
    }

    @Test
    fun nonEmptyStringThreeArgumentsInsufficientQuantityStart() {
        val actual = execSubstring("1", -4, 1)
        assertEquals("", actual)
    }

}