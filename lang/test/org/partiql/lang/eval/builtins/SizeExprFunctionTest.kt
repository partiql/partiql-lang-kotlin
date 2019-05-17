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

import org.junit.*
import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*

class SizeExprFunctionTest : TestBase() {
    private val subject = SizeExprFunction(valueFactory)

    private val env = Environment.standard()

    private fun callSize(vararg arg: ExprValue): Number? = subject.call(env, arg.toList()).scalar.numberValue()

    private fun listOf(vararg values: IonValue): ExprValue = valueFactory.newList(
        values.asSequence().map { valueFactory.newFromIonValue(it) })

    private fun structOf(vararg values: IonValue): ExprValue {
        val struct = ion.newEmptyStruct()
        values.forEachIndexed { index, value -> struct.put(index.toString(), value) }

        return valueFactory.newFromIonValue(struct)
    }

    private fun bagOf(vararg values: IonValue): ExprValue {
        return valueFactory.newBag(values.asSequence().map { valueFactory.newFromIonValue(it) })
    }

    @Test
    fun emptyStruct() = assertEquals(0L, callSize(structOf()))

    @Test
    fun emptyList() = assertEquals(0L, callSize(listOf()))

    @Test
    fun emptyBag() = assertEquals(0L, callSize(bagOf()))

    @Test
    fun singleElementStruct() = assertEquals(1L, callSize(structOf(ion.newString("foo"))))

    @Test
    fun singleElementList() = assertEquals(1L, callSize(listOf(ion.newString("foo"))))

    @Test
    fun singleElementBag() = assertEquals(1L, callSize(bagOf(ion.newString("foo"))))

    @Test
    fun multiElementStruct() = assertEquals(2L, callSize(structOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementList() = assertEquals(2L, callSize(listOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun multiElementBag() = assertEquals(2L, callSize(bagOf(ion.newString("foo"), ion.newString("bar"))))

    @Test
    fun wrongType() = try {
        callSize(valueFactory.newInt(1))
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("invalid argument type for size")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARGUMENT_TYPES]?.stringValue()).isEqualTo("LIST or BAG or STRUCT")
            assertThat(errorContext[Property.ACTUAL_ARGUMENT_TYPES]?.stringValue()).isEqualTo("INT")
            assertThat(errorContext[Property.FUNCTION_NAME]?.stringValue()).isEqualTo("size")
        }
    }

    @Test
    fun lessArguments() = try {
        callSize()
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("size takes a single argument, received: 0")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARITY_MIN]?.integerValue()).isEqualTo(1)
            assertThat(errorContext[Property.EXPECTED_ARITY_MAX]?.integerValue()).isEqualTo(1)
        }
    }

    @Test
    fun moreArguments() = try {
        callSize(structOf(), structOf())
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("size takes a single argument, received: 2")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL)
            assertThat(e.errorContext).isNotNull

            val errorContext = e.errorContext!!
            assertThat(errorContext[Property.EXPECTED_ARITY_MIN]?.integerValue()).isEqualTo(1)
            assertThat(errorContext[Property.EXPECTED_ARITY_MAX]?.integerValue()).isEqualTo(1)
        }
    }
}