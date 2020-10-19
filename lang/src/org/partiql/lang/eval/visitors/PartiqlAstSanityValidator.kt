
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

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.domains.errorContextFrom
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errIntOverflow

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct visitor so that all other visitors may assume that the AST at least passed the
 * checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 *
 * - [org.partiql.lang.syntax.SqlParser]
 * - A visitor transform pass (internal or external)
 *
 */
object PartiqlAstSanityValidator {
    fun validate(statement: PartiqlAst.Statement) {
        when (statement) {
            is PartiqlAst.Statement.Query -> {
                when (val node = statement.expr) {
                    is PartiqlAst.Expr.Lit -> {
                        val ionValue = node.value
                        val metas = node.metas
                        if(node.value is IntElement && ionValue.integerSize == IntElementSize.BIG_INTEGER) {
                            errIntOverflow(errorContextFrom(metas))
                        }
                    }
                    is PartiqlAst.Expr.CallAgg -> {
                        val setQuantifier = node.setq
                        val metas = node.metas
                        if (setQuantifier is PartiqlAst.SetQuantifier.Distinct && metas.containsKey(IsCountStarMeta.TAG)) {
                            err("COUNT(DISTINCT *) is not supported",
                                ErrorCode.EVALUATOR_COUNT_DISTINCT_STAR,
                                errorContextFrom(metas),
                                internal = false)
                        }
                    }
                    is PartiqlAst.Expr.Select -> {
                        val projection = node.project
                        val groupBy = node.group
                        val having = node.having
                        val metas = node.metas

                        if (groupBy != null) {
                            if (groupBy.strategy is PartiqlAst.GroupingStrategy.GroupPartial) {
                                err("GROUP PARTIAL not supported yet",
                                    ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                                    errorContextFrom(metas).also {
                                        it[Property.FEATURE_NAME] = "GROUP PARTIAL"
                                    }, internal = false)
                            }

                            when (projection) {
                                is PartiqlAst.Projection.ProjectPivot -> {
                                    err("PIVOT with GROUP BY not supported yet",
                                        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                                        errorContextFrom(metas).also {
                                            it[Property.FEATURE_NAME] = "PIVOT with GROUP BY"
                                        }, internal = false)
                                }
                                is PartiqlAst.Projection.ProjectValue, is PartiqlAst.Projection.ProjectList -> {
                                    // use of group by with SELECT & SELECT VALUE is supported
                                }
                            }
                        }

                        if ((groupBy == null || groupBy.keyList.keys.isEmpty()) && having != null) {
                            throw SemanticException("HAVING used without GROUP BY (or grouping expressions)",
                                                    ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY,
                                                    PropertyValueMap().addSourceLocation(metas))
                        }
                    }
                    else -> { /* no validation necessary */ }
                }
            }
            else -> { /* no validation necessary */ }
        }
    }
}
