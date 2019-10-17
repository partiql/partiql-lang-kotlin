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

class AstSanityValidatorTests : TestBase() {
    private val dummyMetas = metaContainerOf()

    private fun litInt(value: Int) = Literal(ion.newInt(value), dummyMetas)

    @Test
    fun naryArity_incorrect() {
        val expr =
            NAry(
                NAryOp.NOT,
                listOf(litInt(1), litInt(2)),
                dummyMetas)

        assertThrowsSqlException(ErrorCode.SEMANTIC_INCORRECT_NODE_ARITY) { AstSanityValidator.validate(expr) }
    }

    @Test
    fun dataTypeArity_incorrect() {
        // Can't use the parser to more easily construct an AST here because it will never give us invalid arity.
        val dataType = DataType(SqlDataType.FLOAT, listOf(1, 2), dummyMetas)
        assertThrowsSqlException(ErrorCode.SEMANTIC_INCORRECT_NODE_ARITY) { AstSanityValidator.validate(dataType) }
    }

    @Test
    fun selectProjection_AsteriskNotAlone() {
        // Can't use the parser to more easily construct an AST here because it has checks to prevent this
        // scenario
        val projection =
            SelectProjectionList(
                listOf(
                    SelectListItemStar(dummyMetas),
                    SelectListItemStar(dummyMetas)))

        assertThrowsSqlException(ErrorCode.SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS) { AstSanityValidator.validate(projection) }
    }

    @Test
    fun countDistinct() {
        // Can't use the parser to more easily construct an AST because it doens't support COUNT(DISTINCT <expr>) yet.
        val callAgg = CallAgg(
            VariableReference("foo", CaseSensitivity.INSENSITIVE, ScopeQualifier.UNQUALIFIED, dummyMetas),
            SetQuantifier.DISTINCT,
            Literal(ion.newInt(1), dummyMetas),
            dummyMetas)

        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            AstSanityValidator.validate(callAgg)
        }
    }

    @Test
    fun groupPartial() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            AstSanityValidator.validate(
                Select(setQuantifier = SetQuantifier.ALL,
                       projection = SelectProjectionValue(litInt(1)),
                       from = FromSourceExpr(litInt(1), null),
                       groupBy = GroupBy(
                           GroupingStrategy.PARTIAL, //<-- GroupingStrategy.PARTIAL is not yet supported
                           groupByItems = listOf()),
                        metas = dummyMetas)

            )
        }
    }
    @Test
    fun pivotWithGroupBy() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            AstSanityValidator.validate(
                Select(setQuantifier = SetQuantifier.ALL,
                       projection = SelectProjectionPivot(litInt(1), litInt(1)),
                       from = FromSourceExpr(litInt(1), null),
                       groupBy = GroupBy(
                           GroupingStrategy.FULL,
                           groupByItems = listOf()),
                        metas = dummyMetas)

            )
        }
    }

    @Test
    fun havingWithoutGroupByGroupByIsNull() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            AstSanityValidator.validate(
                Select(setQuantifier = SetQuantifier.ALL,
                       projection = SelectProjectionValue(litInt(1)),
                       from = FromSourceExpr(litInt(1), null),
                       // The error should occur when `groupBy` is null but `having` is not
                       groupBy = null,
                       having = litInt(1),
                       metas = dummyMetas)

            )
        }
    }

    @Test
    fun havingWithoutGroupByNoGroupByItems() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            AstSanityValidator.validate(
                Select(setQuantifier = SetQuantifier.ALL,
                       projection = SelectProjectionValue(litInt(1)),
                       from = FromSourceExpr(litInt(1), null),
                       // The error should occur when `groupBy.groupByItems` is empty and `having` is not null
                       groupBy = GroupBy(GroupingStrategy.FULL, listOf()),
                       having = litInt(1),
                       metas = dummyMetas)

            )
        }
    }

    @Test
    fun literalIntOverflow() {
        val literalInt = Literal(ion.singleValue("${Long.MAX_VALUE}0"), dummyMetas)
        assertThrowsSqlException(ErrorCode.EVALUATOR_INTEGER_OVERFLOW) {
            AstSanityValidator.validate(literalInt)
        }
    }
}
