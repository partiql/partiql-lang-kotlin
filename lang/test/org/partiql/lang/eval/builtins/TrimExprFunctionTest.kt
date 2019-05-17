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
import org.assertj.core.api.Assertions.*
import org.junit.*
import org.junit.Assert.*
import org.partiql.lang.*

/**
 * Tests for [TrimExprFunction], most tests are done e2e through the evaluator, see [BuiltinFunctionsTest]
 */
class TrimExprFunctionTest : TestBase() {
    private val env = Environment.standard()

    private val subject = TrimExprFunction(valueFactory)

    private fun callTrim(vararg args: Any) = subject.call(env, args.map { anyToExprValue(it) }.toList()).stringValue()

    @Test
    fun oneArgument() = assertEquals("string", callTrim("   string   "))

    @Test
    fun twoArguments() = assertEquals("string   ", callTrim("leading", "   string   "))

    @Test
    fun twoArguments2() = assertEquals("string", callTrim("12", "1212string1212"))

    @Test
    fun twoArgumentsBoth() = assertEquals("", callTrim("both", "      "))

    @Test
    fun twoArgumentsLeading() = assertEquals("", callTrim("leading", "      "))

    @Test
    fun twoArgumentsTrailing() = assertEquals("", callTrim("trailing", "      "))

    @Test
    fun threeArguments() = assertEquals("string", callTrim("both", "a", "aaaaaaaaaastringaaaaaaa"))

    @Test
    fun zeroArguments() {
        assertThatThrownBy { callTrim() }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("trim takes between 1 and 3 arguments, received: 0")
    }

    @Test
    fun moreThanThreeArguments() {
        assertThatThrownBy { callTrim("both", "a", "aaaaaaaaaastringaaaaaaa", "a") }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("trim takes between 1 and 3 arguments, received: 4")
    }

    @Test
    fun wrongSpecificationType() {
        assertThatThrownBy { assertEquals("string", callTrim(1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("with two arguments trim's first argument must be either the specification or a 'to remove' string")
    }

    @Test
    fun wrongArgumentType() {
        assertThatThrownBy { assertEquals("string", callTrim(1)) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("Expected text: 1")
    }

    @Test
    fun wrongToRemoveType() {
        assertThatThrownBy { assertEquals("string", callTrim("both", 1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("Expected text: 1")
    }
}