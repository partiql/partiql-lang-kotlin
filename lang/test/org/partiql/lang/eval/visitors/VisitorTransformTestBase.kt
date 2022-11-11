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

package org.partiql.lang.eval.visitors

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.fail
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestFailureReason
import org.partiql.lang.eval.evaluatortestframework.assertThrowsSqlException
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.syntax.PartiQLParserTestBase

/** Provides some basic functionality for parameterized testing implementation of [PartiqlAst.VisitorTransform]. */
abstract class VisitorTransformTestBase : PartiQLParserTestBase() {

    class TransformTestCase() {
        val parser = PartiQLParserBuilder.standard().build()
        var name = ""
        lateinit var original: PartiqlAst.Statement
        lateinit var expected: PartiqlAst.Statement
        constructor(original: String, expected: String, name: String = "") : this() {
            this.original = assertDoesNotThrow("Parsing Original SQL") {
                this.parser.parseAstStatement(original)
            }
            this.expected = assertDoesNotThrow("Parsing Expected SQL") {
                this.parser.parseAstStatement(expected)
            }
            this.name = name
        }

        constructor(original: PartiqlAst.Statement, expected: PartiqlAst.Statement) : this() {
            this.original = original
            this.expected = expected
        }

        constructor(original: PartiqlAst.Statement, expected: String) : this() {
            this.original = original
            this.expected = assertDoesNotThrow("Parsing Expected SQL") {
                this.parser.parseAstStatement(expected)
            }
        }

        override fun toString(): String {
            return "$name --> $original"
        }
    }

    data class TransformErrorTestCase(val query: String, val expectedErrorCode: ErrorCode) {
        internal fun testDetails(actualErrorCode: ErrorCode? = null): String {
            return buildString {
                appendLine("Query               : $query")
                appendLine("Expected Error Code : $expectedErrorCode")
                appendLine("Actual Error Code   : $actualErrorCode")
            }
        }
    }

    /**
     * Similar to [runTest], but executes the transform again a second time on the result of the first transform
     * and ensures that the second result is the same as the first.  This ensures that the transform is idempotent.
     */
    protected fun runTestForIdempotentTransform(tc: TransformTestCase, transform: PartiqlAst.VisitorTransform) {

        val actualAst = transform.transformStatement(tc.original)

        assertEquals("The expected AST must match the transformed AST", tc.expected, actualAst)

        // Idempotent transforms should have the same result if the result of the first pass is passed into a
        // second pass.
        val anotherActualAst = transform.transformStatement(actualAst)
        assertEquals(
            "The second pass of ${transform.javaClass.name} pass should not change the AST",
            actualAst,
            anotherActualAst
        )
    }

    /**
     * Parses [TransformTestCase.originalSql], then runs the specified transformers on the AST.
     * Parses [TransformTestCase.expectedSql], and asserts the transformed AST is equivalent to the expected AST.
     */
    protected fun runTest(tc: TransformTestCase, transformers: List<PartiqlAst.VisitorTransform>) {
        val actualStatement = transformers.fold(tc.original) { node, transform ->
            transform.transformStatement(node)
        }

        assertEquals("The expected AST must match the transformed AST", tc.expected, actualStatement)
    }

    protected fun runErrorTest(tc: TransformErrorTestCase, transform: PartiqlAst.VisitorTransform) {

        val ex = assertThrowsSqlException(
            EvaluatorTestFailureReason.EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE,
            { tc.testDetails() }
        ) {
            val ast = org.junit.jupiter.api.assertDoesNotThrow("Parsing Original SQL") {
                this.parser.parseAstStatement(tc.query)
            }
            transform.transformStatement(ast)
        }

        org.partiql.lang.eval.evaluatortestframework.assertEquals(
            tc.expectedErrorCode,
            ex.errorCode,
            EvaluatorTestFailureReason.UNEXPECTED_ERROR_CODE
        ) { tc.testDetails(actualErrorCode = ex.errorCode) }
    }

    private fun <T> assertDoesNotThrow(message: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            fail("Expected block to not throw but it threw: $message", e)
        }
    }
}
