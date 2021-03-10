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

package org.partiql.lang.ast

import org.partiql.lang.*
import junitparams.*
import org.junit.*

/**
 * This class contains tests for (de)serialization of metas.
 * For tests related to basic structure of the serialized [ExprNode]s, see [org.partiql.lang.syntax.SqlParserTest].
 */
class AstSerDeTests : TestBase() {

    fun parametersForSerDeMetas() = listOf(
        Literal(ion.newInt(1), metaContainerOf(SourceLocationMeta(1, 1, 1)))
    )

    @Test
    @Parameters
    fun serDeMetas(testExprNode: ExprNode) {
        // Convert testExprNode to PartiqlAst.Statement and back to ExprNode and assert the result matches
        val exprToAstToExpr = testExprNode.toAstStatement().toExprNode(ion)
        assertEquals(testExprNode, exprToAstToExpr)
    }
}
