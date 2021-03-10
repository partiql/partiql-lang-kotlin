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

import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.SqlParserTestBase
import org.junit.jupiter.api.fail

/** Provides some basic functionality for parameterized testing implementation of [PartiqlAst.VisitorTransform]. */
abstract class VisitorTransformTestBase : SqlParserTestBase() {

    data class TransformTestCase(val originalSql: String, val expectedSql: String)

    /**
     * Similar to [runTest], but executes the transform again a second time on the result of the first transform
     * and ensures that the second result is the same as the first.  This ensures that the transform is idempotent.
     */
    protected fun runTestForIdempotentTransform(tc: TransformTestCase, transform: PartiqlAst.VisitorTransform) {
        val originalAst = assertDoesNotThrow("Parsing TransformTestCase.originalSql") {
            super.parser.parseExprNode(tc.originalSql).toAstStatement()
        }
        val expectedAst = assertDoesNotThrow("Parsing TransformTestCase.expectedSql") {
            super.parser.parseExprNode(tc.expectedSql).toAstStatement()
        }

        val actualAst = transform.transformStatement(originalAst)

        assertEquals("The expected AST must match the transformed AST", expectedAst, actualAst)

        // Idempotent transforms should have the same result if the result of the first pass is passed into a
        // second pass.
        val anotherActualAst = transform.transformStatement(actualAst)
        assertEquals(
            "The second pass of ${transform.javaClass.name} pass should not change the AST",
            actualAst,
            anotherActualAst)
    }


    /**
     * Parses [TransformTestCase.originalSql], then runs the specified transformers on the AST.
     * Parses [TransformTestCase.expectedSql], and asserts the transformed AST is equivalent to the expected AST.
     */
    protected fun runTest(tc: TransformTestCase, transformers: List<PartiqlAst.VisitorTransform>) {
        val originalAst = assertDoesNotThrow("Parsing TransformTestCase.originalSql") {
            super.parser.parseExprNode(tc.originalSql).toAstStatement()
        }
        val expectedAst = assertDoesNotThrow("Parsing TransformTestCase.expectedSql") {
            super.parser.parseExprNode(tc.expectedSql).toAstStatement()
        }

        val actualExprNode = transformers.fold(originalAst) { node, transform ->
            transform.transformStatement(node)
        }

        assertEquals("The expected AST must match the transformed AST", expectedAst, actualExprNode)
    }

    private fun <T> assertDoesNotThrow(message: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            fail("Expected block to not throw but it threw: $message", e)
        }
    }
}