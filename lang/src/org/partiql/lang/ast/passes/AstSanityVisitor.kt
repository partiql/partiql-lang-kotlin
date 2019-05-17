
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

import com.amazon.ion.*
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct visitor so that all other passes may assume that the AST at least passed the
 * checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 *
 * - [org.partiql.lang.syntax.SqlParser]
 * - A rewrite pass (internal or external)
 *
 * At the time of this writing there are only 3 checks performed, with the idea that more may be added later.  Checks
 * that:
 *
 * - [NAry] arity is correct
 * - [DataType] arity is correct.
 * - `*` is not used in conjunction with other select list items.
 */
class AstSanityVisitor : AstVisitor {

    private fun checkArity(functionName: String, expectedRange: IntRange, actual: Int, metas: MetaContainer) {
        if(!expectedRange.contains(actual)) {
            throw SemanticException(
                "Incorrect arity",
                ErrorCode.SEMANTIC_INCORRECT_NODE_ARITY,
                propertyValueMapOf(
                    Property.FUNCTION_NAME to functionName,
                    Property.EXPECTED_ARITY_MIN to expectedRange.first,
                    Property.EXPECTED_ARITY_MAX to expectedRange.last,
                    Property.ACTUAL_ARITY to actual
                ).addSourceLocation(metas))
        }
    }

    override fun visitExprNode(expr: ExprNode) {
        when (expr) {
            is Literal -> {
                val (ionValue, metas: MetaContainer) = expr
                if(ionValue is IonInt && ionValue.integerSize == IntegerSize.BIG_INTEGER) {
                    errIntOverflow(errorContextFrom(metas))
                }
            }
            is NAry -> {
                val (op, args, _: MetaContainer) = expr
                checkArity(op.symbol, op.arityRange, args.size, expr.metas)
            }
            is CallAgg -> {
                val (_, setQuantifier, _, metas: MetaContainer) = expr
                if (setQuantifier == SetQuantifier.DISTINCT) {
                    err("DISTINCT aggregate function calls not supported",
                        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                        errorContextFrom(metas).also {
                            it[Property.FEATURE_NAME] = "DISTINCT aggregate function calls"
                        }, internal = false)
                }
            }
            is Select -> {
                val (_, projection, _, _, groupBy, having, _, metas) = expr

                if(groupBy != null) {
                    if (groupBy.grouping == GroupingStrategy.PARTIAL) {
                        err("GROUP PARTIAL not supported yet",
                            ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                            errorContextFrom(metas).also {
                                it[Property.FEATURE_NAME] = "GROUP PARTIAL"
                            }, internal = false)
                    }

                    when(projection) {
                        is SelectProjectionPivot -> {
                            err("PIVOT with GROUP BY not supported yet",
                                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                                errorContextFrom(metas).also {
                                    it[Property.FEATURE_NAME] = "PIVOT with GROUP BY"
                                }, internal = false)
                        }
                        is SelectProjectionValue, is SelectProjectionList -> {
                            // use of group by with SELECT & SELECT VALUE is supported
                        }
                    }
                }

                if((groupBy == null || groupBy.groupByItems.isEmpty()) && having != null) {
                    throw SemanticException("HAVING used without GROUP BY (or grouping expressions)",
                        ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY,
                        PropertyValueMap().addSourceLocation(metas))
                }
            }
            else -> { /* intentionally left blank */ }
        }
    }

    override fun visitSelectProjection(projection: SelectProjection) {
        if (projection is SelectProjectionList) {
            val asterisk = projection.items.filterIsInstance<SelectListItemStar>().firstOrNull()

            // If the select list contains more than one item and one of them is a `*`
            if(asterisk != null && projection.items.size > 1) {
                throw SemanticException(
                    "`*` cannot be used with other select list items",
                    ErrorCode.SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS,
                    PropertyValueMap().addSourceLocation(asterisk.metas))
            }
        }
    }

    override fun visitDataType(dataType: DataType) {
        val (sqlDataType, args, _: MetaContainer) = dataType
        if(!sqlDataType.arityRange.contains(args.size)) {
            checkArity(sqlDataType.name, sqlDataType.arityRange, args.size, dataType.metas)
        }
    }
}
