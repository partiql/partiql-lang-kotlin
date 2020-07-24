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


abstract class VisitorTransformTestBase : SqlParserTestBase() {

    data class XformTestCase(val originalSql: String, val expectedSql: String)

    /**
     * Similar to [runTest], but executes the rewriter again a second time on the result of the first rewrite
     * and ensures that the second result is the same as the first.  This ensures that the second
     * pass of the rewrite is idempotent.
     */
    protected fun runTestForIdempotentRewriter(tc: XformTestCase, xformer: PartiqlAst.VisitorTransform) {
        // Note: need to strip metas here because the rewritten node does not have exactly the same source locations
        // as their rewritten counterparts.
        val originalAst = super.parser.parseExprNode(tc.originalSql).toAstStatement()
        val expectedAst = super.parser.parseExprNode(tc.expectedSql).toAstStatement()

        val actualAst = xformer.transformStatement(originalAst)

        assertEquals("The expected AST must match the rewritten AST", expectedAst, actualAst)

        val anotherActualAst = xformer.transformStatement(actualAst)
        assertEquals(
            "The second pass of ${xformer.javaClass.name} pass should be idempotent",
            actualAst,
            anotherActualAst)
    }

    /**
     * Parses [XformTestCase.originalSql], then runs the specified rewriters on the AST.
     * Parses [XformTestCase.expectedSql], and asserts the rewritten AST is equivalent to the expected AST.
     *
     * Before equivalence is checked, strips both trees of all meta nodes meta nodes are taken into account during
     * the equivalence check.
     */
    protected fun runTest(tc: XformTestCase, xformers: List<PartiqlAst.VisitorTransform>) {
        // Note: need to strip metas here because the rewritten node does not have exactly the same source locations
        // as their rewritten counterparts.
        val originalAst = super.parser.parseExprNode(tc.originalSql).toAstStatement()
        val expectedAst = super.parser.parseExprNode(tc.expectedSql).toAstStatement()

        val actualAst = xformers.fold(originalAst) { expr, transform ->
            transform.transformStatement(expr)
        }

        assertEquals("The expected AST must match the rewritten AST", expectedAst, actualAst)
    }
}