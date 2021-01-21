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

package org.partiql.lang.ast.passes

import org.partiql.lang.syntax.*


@Deprecated("New rewriters should implement PIG's PartiqlAst.VisitorTransform and use VisitorTransformTestBase to test")
abstract class RewriterTestBase : SqlParserTestBase() {

    data class RewriterTestCase(val originalSql: String, val expectedSql: String)

    /**
     * Similar to [runTest], but executes the rewriter again a second time on the result of the first rewrite
     * and ensures that the second result is the same as the first.  This ensures that the second
     * pass of the rewrite is idempotent.
     */
    protected fun runTestForIdempotentRewriter(tc: RewriterTestCase, rewriter: AstRewriter) {
        // Note: need to strip metas here because the rewritten node does not have exactly the same source locations
        // as their rewritten counterparts.
        val originalExprNode =
            MetaStrippingRewriter.stripMetas(super.parser.parseExprNode(tc.originalSql))
        val expectedExprNode =
            MetaStrippingRewriter.stripMetas(super.parser.parseExprNode(tc.expectedSql))

        val actualExprNode = MetaStrippingRewriter.stripMetas(rewriter.rewriteExprNode(originalExprNode))

        assertEquals("The expected AST must match the rewritten AST", expectedExprNode, actualExprNode)

        val anotherActualExprNode = MetaStrippingRewriter.stripMetas(rewriter.rewriteExprNode(actualExprNode))
        assertEquals(
            "The second pass of ${rewriter.javaClass.name} pass should be idempotent",
            MetaStrippingRewriter.stripMetas(actualExprNode),
            MetaStrippingRewriter.stripMetas(anotherActualExprNode))
    }

    /**
     * Parses [RewriterTestCase.originalSql], then runs the specified rewriters on the AST.
     * Parses [RewriterTestCase.expectedSql], and asserts the rewritten AST is equivalent to the expected AST.
     *
     * Before equivalence is checked, strips both trees of all meta nodes meta nodes are taken into account during
     * the equivalence check.
     */
    protected fun runTest(tc: RewriterTestCase, rewriters: List<AstRewriter>) {
        // Note: need to strip metas here because the rewritten node does not have exactly the same source locations
        // as their rewritten counterparts.
        val originalExprNode =
            MetaStrippingRewriter.stripMetas(super.parser.parseExprNode(tc.originalSql))
        val expectedExprNode =
            MetaStrippingRewriter.stripMetas(super.parser.parseExprNode(tc.expectedSql))

        val actualExprNode = MetaStrippingRewriter.stripMetas(rewriters.fold(originalExprNode) { expr, rewriter ->
            rewriter.rewriteExprNode(expr)
        })

        assertEquals("The expected AST must match the rewritten AST", expectedExprNode, actualExprNode)
    }
}
