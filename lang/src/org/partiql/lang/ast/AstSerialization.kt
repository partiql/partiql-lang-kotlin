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

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import org.partiql.lang.util.IonWriterContext
import org.partiql.lang.util.case
import org.partiql.lang.util.checkThreadInterrupted
import kotlin.UnsupportedOperationException

/**
 * The current version of the AST.  Update this once every time a new variant of the AST is created.
 */
private val CURRENT_AST_VERSION = AstVersion.V1

class AstSerializer {
    companion object {
        /**
         * Given an instance of ExprNode, return its serialized s-expression form.
         *
         * [exprNode] must be valid as determined by [AstSanityVisitor].
         */
        @JvmStatic
        fun serialize(exprNode: ExprNode, ion: IonSystem): IonSexp {
            val resultSexp = ion.newEmptySexp()
            val writer = ion.newWriter(resultSexp)
            IonWriterContext(writer).apply {
                symbol("ast")
                sexp {
                    symbol("version")
                    int(CURRENT_AST_VERSION.number.toLong())
                }
                sexp {
                    symbol("root")
                    this.writeExprNode(exprNode)
                }
            }
            val ast = resultSexp
            return ast
        }
    }
}

private fun IonWriterContext.writeAsTerm(metas: MetaContainer, block: IonWriterContext.() -> Unit) {
    sexp {
        symbol("term")
        sexp {
            symbol("exp")
            block()
        }
        if(metas.shouldSerialize) {
            sexp {
                symbol("meta")
                metas.serialize(this.writer)
            }
        }
    }
}

private fun IonWriterContext.writeExprNode(expr: ExprNode): Unit =
    writeAsTerm(expr.metas) {
            checkThreadInterrupted()
        sexp {
            when (expr) {
                is Literal           -> case { writeLiteral(expr) }
                is LiteralMissing    -> case { writeLiteralMissing(expr) }
                is VariableReference -> case { writeVariableReference(expr) }
                is NAry              -> case { writeNAry(expr) }
                is CallAgg           -> case { writeCallAgg(expr) }
                is Typed             -> case { writeTyped(expr) }
                is Path              -> case { writePath(expr) }
                is SimpleCase        -> case { writeSimpleCase(expr) }
                is SearchedCase      -> case { writeSearchedCase(expr) }
                is Struct            -> case { writeStruct(expr) }
                is ListExprNode      -> case { writeListExprNode(expr) }
                is Bag               -> case { writeBag(expr) }
                is Select            -> case { writeSelect(expr) }
            }.toUnit()
        }
    }

private fun IonWriterContext.writeLiteral(expr: Literal) {
    val (ionValue, _: MetaContainer) = expr
    symbol("lit")
    value(ionValue)
}

private fun IonWriterContext.writeLiteralMissing(expr: LiteralMissing) {
    val (_: MetaContainer) = expr
    symbol("missing")
}

private fun IonWriterContext.writeVariableReference(expr: VariableReference) {
    val (id, sensitivity, lookup, _: MetaContainer) = expr
    fun writeVarRef() {
        symbol("id")
        symbol(id)
        symbol(sensitivity.toSymbol())
    }
    if (lookup == ScopeQualifier.LEXICAL) {
        symbol("@")
        sexp {
            writeVarRef()
        }
    }
    else {
        writeVarRef()
    }
}

private fun IonWriterContext.writeCallAgg(expr: CallAgg) {
    val (funcExpr, setQuantifier, arg, metas: MetaContainer) = expr
    when {
        metas.hasMeta(IsCountStarMeta.TAG) -> {
            symbol("call_agg_wildcard")
            symbol("count")
        }
        else -> {
            symbol("call_agg")
            symbol(funcExpr.getFuncName())
            symbol(setQuantifier.toString().toLowerCase())
            writeExprNode(arg)
        }

    }
}

private fun IonWriterContext.writeTyped(expr: Typed) {
    val (op: TypedOp, exp, dataType, _: MetaContainer) = expr
    symbol(op.text)
    fun writeTypedDetails() {
        writeAsTerm(dataType.metas) {
            sexp {
                symbol("type")
                symbol(dataType.sqlDataType.typeName)
                if (dataType.args.isNotEmpty()) {
                    dataType.args.forEach {
                        int(it)
                    }
                }
            }
        }
    }
    writeExprNode(exp)
    writeTypedDetails()
}

private fun IonWriterContext.writeStruct(expr: Struct) {
    val (fields, _: MetaContainer) = expr
    symbol("struct")
    fields.forEach {
        val (name, valueExpr) = it
        writeExprNode(name)
        writeExprNode(valueExpr)
    }
}

private fun IonWriterContext.writeListExprNode(expr: ListExprNode) {
    val (items, _: MetaContainer) = expr
    symbol("list")
    items.forEach {
        writeExprNode(it)
    }
}

private fun IonWriterContext.writeBag(expr: Bag) {
    val (items, _: MetaContainer) = expr
    symbol("bag")
    items.forEach {
        writeExprNode(it)
    }
}

private fun IonWriterContext.writeSelect(expr: Select) {
    val (setQuantifier, projection, from, where, groupBy, having, limit, _: MetaContainer) = expr

    writeSelectProjection(projection, setQuantifier)

    sexp {
        symbol("from")
        writeFromSource(from)
    }

    where?.let {
        sexp {
            symbol("where")
            writeExprNode(it)
        }
    }

    groupBy?.let {
        val (grouping, groupByItems, groupName) = it

        sexp {
            symbol(
                    when (grouping) {
                        GroupingStrategy.FULL    -> "group"
                        GroupingStrategy.PARTIAL -> "group_partial"
                    }
            )
            sexp {
                symbol("by")
                groupByItems.forEach { gbi ->
                    val (itemExpr, asName) = gbi
                    if (asName != null) {
                        writeAsTerm(asName.metas) {
                            sexp {
                                symbol("as")
                                symbol(asName.name)
                                writeExprNode(itemExpr)
                            }
                        }
                    }
                    else {
                        writeExprNode(itemExpr)
                    }
                }
            }
            groupName?.let { gn ->
                writeAsTerm(gn.metas) {
                    sexp {
                        symbol("name")
                        symbol(gn.name)
                    }
                }
            }
        }
    }

    having?.let {
        sexp {
            symbol("having")
            writeExprNode(it)
        }
    }

    limit?.let {
        sexp {
            symbol("limit")
            writeExprNode(limit)
        }
    }
}

private fun IonWriterContext.writeSelectProjection(projection: SelectProjection, setQuantifier: SetQuantifier) {
    when (projection) {
        is SelectProjectionValue -> case { writeSelectProjectionValue(projection, setQuantifier) }
        is SelectProjectionPivot -> case { writeSelectProjectionPivot(projection) }
        is SelectProjectionList  -> case { writeSelectProjectionList(projection, setQuantifier) }
    }.toUnit()
}

private fun IonWriterContext.writeSelectProjectionValue(
    projection: SelectProjectionValue,
    setQuantifier: SetQuantifier) {
    val (valueExpr) = projection
    symbol("select")
    sexp {
        symbol(
            when (setQuantifier) {
                SetQuantifier.ALL      -> "project"
                SetQuantifier.DISTINCT -> "project_distinct"
            }
        )
        sexp {
            symbol("value")
            writeExprNode(valueExpr)
        }
    }
}

private fun IonWriterContext.writeSelectProjectionPivot(projection: SelectProjectionPivot) {
    val (asExpr, atExpr) = projection
    symbol("pivot")

    sexp {
        symbol("member")
        writeExprNode(asExpr)
        writeExprNode(atExpr)
    }
}

private fun IonWriterContext.writeSelectProjectionList(
    projection: SelectProjectionList,
    setQuantifier: SetQuantifier) {
    val (items) = projection
    symbol("select")

    sexp {
        symbol(
            when (setQuantifier) {
                SetQuantifier.ALL      -> "project"
                SetQuantifier.DISTINCT -> "project_distinct"
            }
        )

        sexp {
            symbol("list")
            items.forEach {
                when (it) {
                    is SelectListItemStar       -> case { writeSelectListItemStar(it) }
                    is SelectListItemExpr       -> case { writeSelectListItemExpr(it) }
                    is SelectListItemProjectAll -> case { writeSelectListItemProjectAll(it) }
                }.toUnit()
            }
        }
    }
}

private fun IonWriterContext.writeSelectListItemStar(it: SelectListItemStar) {
    writeAsTerm(it.metas) {
        sexp {
            symbol("star")
        }
    }
}

private fun IonWriterContext.writeSelectListItemExpr(it: SelectListItemExpr) {
    val (itemExpr, asName) = it
    if (asName != null) {
        writeAsTerm(asName.metas) {
            sexp {
                symbol("as")
                symbol(asName.name)
                writeExprNode(itemExpr)
            }
        }
    }
    else {
        writeExprNode(itemExpr)
    }
}

private fun IonWriterContext.writeSelectListItemProjectAll(it: SelectListItemProjectAll) {
    val (exp) = it
    sexp {
        symbol("path_project_all")
        writeExprNode(exp)
    }
}

private fun IonWriterContext.writeFromSource(fromSource: FromSource): Unit =
    when (fromSource) {
        is FromSourceExpr    -> case {
            val (exp, asName, atName) = fromSource
            nestAsAt(asName, atName) {
                writeExprNode(exp)
            }
        }
        is FromSourceUnpivot -> case {
            val (exp, asName, atName, metas) = fromSource
            nestAsAt(asName, atName) {
                writeAsTerm(metas) {
                    sexp {
                        symbol("unpivot")
                        writeExprNode(exp)
                    }
                }
            }
        }
        is FromSourceJoin    -> case {
            val (op, leftRef, rightRef, condition) = fromSource
            writeAsTerm(fromSource.metas) {
                sexp {
                    symbol(
                        when (op) {
                            JoinOp.INNER -> "inner_join"
                            JoinOp.LEFT  -> "left_join"
                            JoinOp.RIGHT -> "right_join"
                            JoinOp.OUTER -> "outer_join"
                        }
                    )
                    writeFromSource(leftRef)
                    writeFromSource(rightRef)

                    if (!fromSource.metas.hasMeta(IsImplictJoinMeta.TAG)) {
                        writeExprNode(condition)
                    }
                }
            }
        }
    }.toUnit()

private fun IonWriterContext.writeSimpleCase(expr: SimpleCase) {
    symbol("simple_case")
    val (valueExpr, whenClauses, elseExpr, _: MetaContainer) = expr
    writeExprNode(valueExpr)
    whenClauses.forEach {
        val (whenValueExpr, thenExpr) = it
        sexp {
            symbol("when")
            writeExprNode(whenValueExpr)
            writeExprNode(thenExpr)
        }
    }
    elseExpr?.let {
        sexp {
            symbol("else")
            writeExprNode(it)
        }
    }
}

private fun IonWriterContext.writeSearchedCase(expr: SearchedCase) {
    symbol("searched_case")
    val (whenClauses, elseExpr, _: MetaContainer) = expr
    whenClauses.forEach {
        val (condition, thenExpr) = it
        sexp {
            symbol("when")
            writeExprNode(condition)
            writeExprNode(thenExpr)
        }
    }
    elseExpr?.let {
        sexp {
            symbol("else")
            writeExprNode(it)
        }
    }
}

private fun IonWriterContext.writePath(expr: Path) {
    val (root, components, _: MetaContainer) = expr
    symbol("path")
    writeExprNode(root)
    components.forEach {
        sexp {
            symbol("path_element")
            when (it) {
                is PathComponentExpr     -> case { writePathComponentExpr(it) }
                is PathComponentUnpivot  -> case { writePathComponentUnpivot(it) }
                is PathComponentWildcard -> case { writePathComponentWildcard(it) }
            }.toUnit()
        }
    }
}

private fun IonWriterContext.writePathComponentExpr(it: PathComponentExpr) {
    val (exp, case) = it
    writeExprNode(exp)
    symbol(case.toSymbol())
}

private fun IonWriterContext.writePathComponentUnpivot(it: PathComponentUnpivot) {
    writeAsTerm(it.metas) {
        sexp {
            symbol("star")
            symbol("unpivot")
        }
    }
}

private fun IonWriterContext.writePathComponentWildcard(it: PathComponentWildcard) {
    writeAsTerm(it.metas) {
        sexp {
            symbol("star")
        }
    }
}

private fun IonWriterContext.writeNAry(expr: NAry) {
    val (op, args, _: MetaContainer) = expr

    symbol(op.symbol)
    when(op) {
        NAryOp.CALL -> {
            // Note: we can assume that by this point the AST has been checked
            // for errors.  (in this case that means the arity is at least 1)
            // This means it is safe to access the first element of args below.
            val funcExpr = expr.args.first()
            val funcName = funcExpr.getFuncName()
            symbol(funcName)
            args.drop(1).forEach {
                writeExprNode(it)
            }
        }
        else -> {
            args.forEach {
                writeExprNode(it)
            }
        }
    }
}

private fun IonWriterContext.nestAsAt(asName: SymbolicName?, atName: SymbolicName?, block: () -> Unit) {

    when {
        asName == null && atName == null -> block()
        asName == null && atName != null -> {
            writeAsTerm(atName.metas) {
                sexp {
                    symbol("at")
                    symbol(atName.name)
                    block()
                }
            }
        }
        asName != null && atName == null ->
            writeAsTerm(asName.metas) {
                sexp {
                    symbol("as")
                    symbol(asName.name)
                    block()
                }
            }
        asName != null && atName != null ->
            writeAsTerm(atName.metas) {
                sexp {
                    symbol("at")
                    symbol(atName.name)
                    writeAsTerm(asName.metas) {
                        sexp {
                            symbol("as")
                            symbol(asName.name)
                            block()
                        }
                    }
                }
        }
    }
}



private fun ExprNode.getFuncName(): String {
    return when (this) {
        is VariableReference -> id
        else -> throw UnsupportedOperationException(
            "Using arbitrary expressions to identify a function in a call_agg or call node is not supported. " +
            "Functions must be identified by name only.")
    }
}


