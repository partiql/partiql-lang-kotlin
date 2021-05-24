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

import org.partiql.lang.ast.*
import org.partiql.lang.util.*

/**
 * Contains the logic necessary to walk every node in the AST and invokes methods of [AstVisitor] along the way.
 */
open class AstWalker(private val visitor: AstVisitor) {

    fun walk(exprNode: ExprNode) {
        walkExprNode(exprNode)
    }

    protected open fun walkExprNode(vararg exprs: ExprNode?) {
        exprs.filterNotNull().forEach { expr: ExprNode ->
            checkThreadInterrupted()
            visitor.visitExprNode(expr)

            when (expr) {
                is Literal,
                is LiteralMissing,
                is VariableReference,
                is Parameter     -> case {
                    // Leaf nodes have no children to walk.
                }
                is NAry         -> case {
                    val (_, args, _: MetaContainer) = expr
                    args.forEach { it ->
                        walkExprNode(it)
                    }
                }
                is CallAgg      -> case {
                    val (funcExpr, _, arg, _: MetaContainer) = expr
                    walkExprNode(funcExpr)
                    walkExprNode(arg)
                }
                is Typed        -> case {
                    val (_, exp, sqlDataType, _: MetaContainer) = expr
                    walkExprNode(exp)
                    visitor.visitDataType(sqlDataType)
                }
                is Path         -> case {
                    walkPath(expr)
                }
                is SimpleCase   -> case {
                    val (valueExpr, branches, elseExpr, _: MetaContainer) = expr
                    walkExprNode(valueExpr)
                    branches.forEach {
                        val (branchValueExpr, thenExpr) = it
                        walkExprNode(branchValueExpr, thenExpr)
                    }
                    walkExprNode(elseExpr)
                }
                is SearchedCase -> case {
                    val (branches, elseExpr, _: MetaContainer) = expr
                    branches.forEach {
                        val (conditionExpr, thenExpr) = it
                        walkExprNode(conditionExpr, thenExpr)
                    }
                    walkExprNode(elseExpr)
                }
                is Struct       -> case {
                    val (fields, _: MetaContainer) = expr
                    fields.forEach {
                        val (nameExpr, valueExpr) = it
                        walkExprNode(nameExpr, valueExpr)
                    }
                }
                is Seq          -> case {
                    val (_, items, _: MetaContainer) = expr
                    items.forEach {
                        walkExprNode(it)
                    }
                }
                is Select       -> case {
                    val (_, projection, from, fromLet, where, groupBy, having, orderBy, limit, _: MetaContainer) = expr
                    walkSelectProjection(projection)
                    walkFromSource(from)
                    walkExprNode(where)
                    groupBy?.let {
                        val (_, groupByItems, _) = it

                        groupByItems.forEach { gbi ->
                            val (groupExpr) = gbi
                            walkExprNode(groupExpr)
                        }
                    }
                    walkExprNode(having)
                    orderBy?.let {
                        it.sortSpecItems.forEach { ssi ->
                            walkExprNode(ssi.expr)
                        }
                    }
                    walkExprNode(limit)
                }
                is DataManipulation -> case {
                    val (dmlOperation, from, where, returning, _: MetaContainer) = expr
                    walkDmlOperations(dmlOperation)
                    if (from != null) {
                        walkFromSource(from)
                    }
                    walkExprNode(where)
                    returning?.let {
                        it.returningElems.forEach { re ->
                            when (re.columnComponent) {
                                is ReturningColumn   -> case {
                                    walkExprNode(re.columnComponent.column)
                                }
                                is ReturningWildcard -> case {
                                    //Leaf nodes have no children to walk.
                                }
                            }
                        }
                    }
                }
                is CreateIndex -> case {
                    val (_, keys, _: MetaContainer) = expr
                    for (key in keys) {
                        walkExprNode(key)
                    }
                }
                is CreateTable, is DropTable, is DropIndex,
                is Exec, is DateTimeType -> case { }
            }.toUnit()
        }
    }

    private fun walkPath(expr: Path) {
        val (root, components) = expr
        walkExprNode(root)
        components.forEach {
            visitor.visitPathComponent(it)
            when (it) {
                is PathComponentUnpivot,
                is PathComponentWildcard -> case {
                    //Leaf nodes have no children to walk.
                }
                is PathComponentExpr     -> case {
                    val (exp) = it
                    walkExprNode(exp)
                }
            }.toUnit()
        }
    }

    private fun walkFromSource(fromSource: FromSource) {
        visitor.visitFromSource(fromSource)
        when (fromSource) {
            is FromSourceExpr    -> case {
                val (exp, _) = fromSource
                walkExprNode(exp)
            }
            is FromSourceUnpivot -> case {
                val (exp, _, _) = fromSource
                walkExprNode(exp)
            }
            is FromSourceJoin    -> case {
                val (_, leftRef, rightRef, condition, _: MetaContainer) = fromSource
                walkFromSource(leftRef)
                walkFromSource(rightRef)
                walkExprNode(condition)
            }
        }.toUnit()
    }

    private fun walkSelectProjection(projection: SelectProjection) {
        visitor.visitSelectProjection(projection)
        when (projection) {
            is SelectProjectionValue -> case {
                val (valueExpr) = projection
                walkExprNode(valueExpr)
            }
            is SelectProjectionPivot -> case {
                val (asExpr, atExpr) = projection
                walkExprNode(asExpr, atExpr)
            }
            is SelectProjectionList  -> case {
                val (items) = projection
                items.forEach {
                    visitor.visitSelectListItem(it)
                    when (it) {
                        is SelectListItemStar       -> case {
                            //Leaf nodes have no children to walk.
                        }
                        is SelectListItemExpr       -> case {
                            walkExprNode(it.expr)
                        }
                        is SelectListItemProjectAll -> case {
                            walkExprNode(it.expr)
                        }
                    }.toUnit()
                }
            }
        }.toUnit()
    }

    private fun walkDmlOperations(dmlOperations: DmlOpList) =
        dmlOperations.ops.forEach {
            walkDmlOperation(it)
        }

    private fun walkDmlOperation(dmlOperation: DataManipulationOperation) {
        when (dmlOperation) {
            is InsertOp -> case {
                val (lValue, values) = dmlOperation
                walkExprNode(lValue, values)
            }
            is InsertValueOp -> case {
                val (lvalue, value, position, onConflict) = dmlOperation
                walkExprNode(lvalue, value, position)
                walkOnConflict(onConflict)
            }
            is AssignmentOp -> case {
                val (assignment) = dmlOperation
                walkExprNode(assignment.lvalue)
                walkExprNode(assignment.rvalue)
            }
            is RemoveOp -> case {
                val (lvalue) = dmlOperation
                walkExprNode(lvalue)
            }
            is DeleteOp -> case {
                // no-op - implicit target
            }
        }.toUnit()
    }

    private fun walkOnConflict(onConflict: OnConflict?) {
        if (onConflict != null) {
            visitor.visitOnConflict(onConflict)
            val (condition, conflictAction) = onConflict
            walkExprNode(condition)
            when (conflictAction) {
                ConflictAction.DO_NOTHING -> {
                }
            }
        }
    }
}
