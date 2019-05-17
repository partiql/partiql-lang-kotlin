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

import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.junit.*

class AstSanityInspectorTests : TestBase() {
    private val dummyMetas = metaContainerOf()

    private val inspector = AstSanityVisitor()
    private fun litInt(value: Int) = Literal(ion.newInt(value), dummyMetas)

    @Test
    fun naryArity_incorrect() {
        val expr =
            NAry(
                NAryOp.NOT,
                listOf(litInt(1), litInt(2)),
                dummyMetas)

        assertThrowsSqlException(ErrorCode.SEMANTIC_INCORRECT_NODE_ARITY) { inspector.visitExprNode(expr) }
    }

    @Test
    fun dataTypeArity_incorrect() {
        val dataType = DataType(SqlDataType.FLOAT, listOf(1, 2), dummyMetas)
        assertThrowsSqlException(ErrorCode.SEMANTIC_INCORRECT_NODE_ARITY) { inspector.visitDataType(dataType) }
    }

    @Test
    fun selectProjection_AsteriskNotAlone() {
        val projection =
            SelectProjectionList(
                listOf(
                    SelectListItemStar(dummyMetas),
                    SelectListItemStar(dummyMetas)))

        assertThrowsSqlException(ErrorCode.SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS) { inspector.visitSelectProjection(projection) }
    }
}
