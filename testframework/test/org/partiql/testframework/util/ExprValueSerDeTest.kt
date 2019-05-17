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

package org.partiql.testframework.util

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import junitparams.*
import org.junit.*
import org.junit.runner.*
import org.partiql.lang.*
import kotlin.test.*


@RunWith(JUnitParamsRunner::class)
class ExprValueSerDeTest {

    data class TestCase(val sql: String, val serializedExprValue: String)

    fun parametersForSerializeAndDeserialize() = listOf(
        TestCase("1", "1"),
        TestCase("`1`", "1"),
        TestCase("[1]", "[1]"),
        TestCase("`[1]`", "[1]"),
        TestCase("[1, 2]", "[1, 2]"),
        TestCase("{ 'a' : 1 }", "{ a: 1 }"),
        TestCase("`{ a : 1 }`", "{ a: 1 }"),
        TestCase("`null`", "null"),
        TestCase("`null.null`", "null"),
        TestCase("`null`", "null.null"),
        TestCase("`null.null`", "null.null"),
        TestCase("`null.bool`", "null.bool"),
        TestCase("`null.int`", "null.int"),
        TestCase("`null.float`", "null.float"),
        TestCase("`null.decimal`", "null.decimal"),
        TestCase("`null.timestamp`", "null.timestamp"),
        TestCase("`null.symbol`", "null.symbol"),
        TestCase("`null.string`", "null.string"),
        TestCase("`null.clob`", "null.clob"),
        TestCase("`null.blob`", "null.blob"),
        TestCase("`null.list`", "null.list"),
        TestCase("`null.sexp`", "null.sexp"),
        TestCase("`null.struct`", "null.struct"),
        TestCase("MISSING", "(missing)"),
        TestCase("{ 'a': 1, 'b': MISSING }", "{ a: 1, b: (missing) }"),
        TestCase("`(1 2)`", "(sexp 1 2)"),
        TestCase("<<1>>", "(bag 1)"),
        TestCase("<<<<1>>>>", "(bag (bag 1))"),
        TestCase("<<1, 2>>", "(bag 1 2)"),
        TestCase("<<1, 2, 2, 3, 3, 3>>", "(bag 1 2 2 3 3 3)"),
        TestCase("<<{'a': 1}, {'a': 2}>>", "(bag {a: 1} {a: 2})"),
        TestCase("[<<1, 2, 2>>]", "[(bag 1 2 2)]"),
        TestCase("[`(some sexp)`]", "[(sexp some sexp)]"),
        TestCase("`(sexp sexp)`", "(sexp sexp sexp)"),
        TestCase("`(term (exp (literal 1)) (meta foo))`", "(sexp term (sexp exp (sexp literal 1)) (sexp meta foo))")
    )

    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)

    private val compilerPipeline = CompilerPipeline.build(ion) {
        compileOptions {
            projectionIteration(ProjectionIterationBehavior.UNFILTERED)
        }
    }

    private fun assertExprEquals(expected: ExprValue, actual: ExprValue, message: String) {
        if (!expected.exprEquals(actual)) {
            println("Expected: ${serializeExprValue(expected)}")
            println("Actual  : ${serializeExprValue(actual)}")

            fail("Expected and actual ExprValues must be equivalent:  $message")
        }
    }

    /**
     * Evaluates [TestCase.sql] to obtain an [ExprValue], serializes it, then makes sure that
     * it is equivalent to [TestCase.serializedExprValue].
     *
     * Also deserializes [TestCase.serializedExprValue] and makes sure it is equivalent to the
     * [ExprValue] resulting from evaluating [TestCase.sql].
     */
    @Test
    @Parameters
    fun serializeAndDeserialize(tc: TestCase) {
        val serializedExprValue = ion.singleValue(tc.serializedExprValue)

        val sqlResult = compilerPipeline.compile(tc.sql).eval(EvaluationSession.builder().build())
        val serializedSqlResult = serializeExprValue(sqlResult)

        assertEquals(serializedSqlResult, serializedExprValue,
            "serializedSqlResult must match serializedExprValue")

        val deserialziedExprValue = deserializeExprValue(serializedExprValue, valueFactory)

        assertExprEquals(
            sqlResult, deserialziedExprValue,
            "sqlResult must match deserialziedExprValue")
    }
}