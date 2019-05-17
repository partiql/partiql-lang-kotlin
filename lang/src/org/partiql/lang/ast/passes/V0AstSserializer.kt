

package org.partiql.lang.ast.passes

import com.amazon.ion.*
import org.partiql.lang.ast.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*
import java.lang.UnsupportedOperationException

/**
 * This class is intended for backward compatibility.  It can serialize any [ExprNode] instance to a V0 s-expression
 * based AST.
 *
 * The [AstSerializer] class will always serialize to the latest version of the s-expression based AST and should be
 * used wherever possible.
 */
@Deprecated("Please use AstSerializer class instead")
class V0AstSerializer(private val writerContext: IonWriterContext) {

    companion object {
        /**
         * Converts an instance of [ExprNode] to the legacy s-expression based AST.
         */
        @JvmStatic
        fun serialize(expr: ExprNode, ion: IonSystem): IonSexp {
            val sepx = ion.newEmptySexp()
            val writer = ion.newWriter(sepx)
            val wc = IonWriterContext(writer)
            serialize(expr, wc)
            val legacyAst = sepx.first() as IonSexp
            return legacyAst
        }

        /**
         * Writes an instance of [ExprNode] to the legacy s-expression based AST
         * to the specified [IonWriterContext].
         */
        @JvmStatic
        fun serialize(expr: ExprNode, wc: IonWriterContext) {
            V0AstSerializer(wc).write(expr)
        }

        private val SetQuantifier.toTagName get() = when (this) {
            SetQuantifier.ALL      -> "project"
            SetQuantifier.DISTINCT -> "project_distinct"
        }
    }

    fun write(expr: ExprNode) {
        convertExprNode(expr)
    }

    private fun <T: HasMetas> meta(node: T, block: (T) -> Unit) {
        (node.metas.find(SourceLocationMeta.TAG) as? SourceLocationMeta)?.let {
            writerContext.sexp {
                symbol("meta")
                block(node)
                struct {
                    int("line", it.lineNum)
                    int("column", it.charOffset)
                }
            }
            return
        }
        block(node)
    }

    private fun convertExprNode(expr: ExprNode): Unit =
        meta(expr) {
            writerContext.sexp {
                when (expr) {
                    is Literal           -> case {
                        val (ionValue, _: MetaContainer) = expr
                        if (ionValue.type == ExprValueType.MISSING) {
                            symbol("missing")
                        } else {
                            symbol("lit")
                            value(ionValue)
                        }
                    }
                    is LiteralMissing    -> case {
                        symbol("missing")
                    }
                    is VariableReference -> case {
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
                        } else {
                            writeVarRef()
                        }
                    }
                    is NAry              -> case {
                        convertNAry(expr)
                    }
                    is CallAgg           -> case {
                        val (funcExpr, setQuantifier, arg, metas: MetaContainer) = expr
                        if(metas.hasMeta(IsCountStarMeta.TAG)) {
                            symbol("call_agg_wildcard")
                            symbol("count")
                        } else {
                            symbol("call_agg")
                            symbol(funcExpr.getFuncName())
                            symbol(setQuantifier.toString().toLowerCase())
                            convertExprNode(arg)
                        }
                    }
                    is Typed              -> case {
                        val (op, exp, dataType, _: MetaContainer) = expr
                        symbol(op.text)
                        convertExprNode(exp)
                        convertDataType(dataType)
                    }
                    is Path              -> case {
                        convertPath(expr)
                    }
                    is SimpleCase        -> case {
                        convertSimpleCase(expr)
                    }
                    is SearchedCase      -> case {
                        convertSearchedCase(expr)
                    }
                    is Struct            -> case {
                        val (fields, _: MetaContainer) = expr
                        symbol("struct")
                        fields.forEach {
                            val (nameExpr, valueExpr) = it
                            convertExprNode(nameExpr)
                            convertExprNode(valueExpr)
                        }
                    }
                    is ListExprNode      -> case {
                        val (items, _: MetaContainer) = expr
                        symbol("list")
                        items.forEach {
                            convertExprNode(it)
                        }
                    }
                    is Bag               -> case {
                        val (items, _: MetaContainer) = expr
                        symbol("bag")
                        items.forEach {
                            convertExprNode(it)
                        }
                    }

                    is Select            -> case {
                        convertSelect(expr)

                    }
                }.toUnit()
            }
        }


    private fun IonWriterContext.convertSelect(expr: Select) {

        val (setQuantifier, projection, from, where, groupBy, having, limit, _: MetaContainer) = expr
        convertSelectProjection(projection, setQuantifier)

        sexp {
            symbol("from")
            convertFromSource(from)
        }

        where?.let {
            sexp {
                symbol("where")
                convertExprNode(it)
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
                    groupByItems.forEach {
                        val (itemExpr, asName) = it
                        if (asName != null) {
                            meta(asName) {
                                sexp {
                                    symbol("as")
                                    symbol(asName.name)
                                    convertExprNode(itemExpr)
                                }
                            }
                        }
                        else {
                            convertExprNode(itemExpr)
                        }
                    }
                }
                if (groupName != null) {
                    meta(groupName) {
                        sexp {
                            symbol("name")
                            symbol(groupName.name)
                        }
                    }
                }
            }

        }

        having?.let {
            sexp {
                symbol("having")
                convertExprNode(it)
            }
        }

        limit?.let {
            sexp {
                symbol("limit")
                convertExprNode(limit)
            }
        }
    }


    private fun IonWriterContext.convertSelectProjection(projection: SelectProjection, setQuantifier: SetQuantifier) {
        when (projection) {
            is SelectProjectionValue -> case {
                val (valueExpr) = projection
                symbol("select")
                sexp {
                    symbol(setQuantifier.toTagName)
                    sexp {
                        symbol("value")
                        convertExprNode(valueExpr)
                    }
                }
            }
            is SelectProjectionPivot -> case {
                val (asExpr, atExpr) = projection
                symbol("pivot")

                sexp {
                    symbol("member")
                    convertExprNode(asExpr)
                    convertExprNode(atExpr)
                }
            }
            is SelectProjectionList  -> case {
                val (items) = projection
                symbol("select")

                sexp {
                    symbol(setQuantifier.toTagName)
                    sexp {
                        symbol("list")
                        items.forEach {
                            when (it) {
                                is SelectListItemStar -> case {
                                    meta(it) {
                                        sexp {
                                            symbol("project_all")
                                        }
                                    }
                                }
                                is SelectListItemExpr -> case {
                                    val (itemExpr, asName) = it
                                    if (asName != null) {
                                        meta(asName) {
                                            sexp {
                                                symbol("as")
                                                symbol(asName.name)
                                                convertExprNode(itemExpr)
                                            }
                                        }
                                    } else {
                                        convertExprNode(itemExpr)
                                    }
                                }
                                is SelectListItemProjectAll -> case {
                                    sexp {
                                        symbol("project_all")
                                        convertExprNode(it.expr)
                                    }
                                }
                            }.toUnit()
                        }
                    }
                }
            }
        }.toUnit()
    }

    private fun IonWriterContext.nestAsAt(asName: SymbolicName?, atName: SymbolicName?, block: () -> Unit) {
        when {
            atName == null && asName == null -> block()
            atName == null && asName != null -> {
                meta(asName) {
                    sexp {
                        symbol("as")
                        symbol(asName.name)
                        block()
                    }
                }
            }
            atName != null && asName == null -> {
                meta(atName) {
                    sexp {
                        symbol("at")
                        symbol(atName.name)
                        block()
                    }
                }
            }
            atName != null && asName != null -> {
                meta(atName) {
                    sexp {
                        symbol("at")
                        symbol(atName.name)
                        meta(asName) {
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
    }

    private fun convertFromSource(fromSource: FromSource) {
        writerContext.apply {
            when (fromSource) {
                is FromSourceExpr    -> case {
                    val (exp, asName, atName) = fromSource
                    nestAsAt(asName, atName) {
                        convertExprNode(exp)
                    }
                }
                is FromSourceUnpivot -> case {
                    val (exp, asName, atName, _: MetaContainer) = fromSource
                    nestAsAt(asName, atName) {
                        meta(fromSource) {
                            sexp {
                                symbol("unpivot")
                                convertExprNode(exp)
                            }
                        }
                    }
                }
                is FromSourceJoin    -> case {
                    meta(fromSource) {
                        val (op, leftRef, rightRef, condition) = fromSource
                        sexp {
                            symbol(
                                when (op) {
                                    JoinOp.INNER -> "inner_join"
                                    JoinOp.LEFT  -> "left_join"
                                    JoinOp.RIGHT -> "right_join"
                                    JoinOp.OUTER -> "outer_join"
                                }
                            )
                            convertFromSource(leftRef)
                            convertFromSource(rightRef)

                            if (!fromSource.metas.hasMeta(IsImplictJoinMeta.TAG)) {
                                convertExprNode(condition)
                            }
                        }
                    }
                }
            }.toUnit()
        }
    }


    private fun IonWriterContext.convertSearchedCase(expr: SearchedCase) {
        val (branches, elseExpr, _: MetaContainer) = expr
        symbol("searched_case")
        branches.forEach {
            val (caseConditionExpr, thenExpr) = it
            sexp {
                symbol("when")
                convertExprNode(caseConditionExpr)
                convertExprNode(thenExpr)
            }
        }
        if (elseExpr != null) {
            sexp {
                symbol("else")
                convertExprNode(elseExpr)
            }
        }
    }

    private fun IonWriterContext.convertSimpleCase(expr: SimpleCase) {
        val (valueExpr, branches, elseExpr, _: MetaContainer) = expr
        symbol("simple_case")
        convertExprNode(valueExpr)
        branches.forEach {
            val (caseValueExpr, thenExpr) = it
            sexp {
                symbol("when")
                convertExprNode(caseValueExpr)
                convertExprNode(thenExpr)
            }
        }
        if (elseExpr != null) {
            sexp {
                symbol("else")
                convertExprNode(elseExpr)
            }
        }
    }

    private fun IonWriterContext.convertPath(expr: Path) {
        val (root, components, _: MetaContainer) = expr
        symbol("path")
        convertExprNode(root)
        components.forEach {
            when (it) {
                is PathComponentExpr     -> case {
                    val (exp, case) = it

                    //Only wrap variable references and literal strings in case_[in]sensitive...
                    if ((exp is VariableReference) || exp is Literal && exp.ionValue is IonString) {
                        sexp {
                            symbol(case.toSymbol())
                            convertExprNode(exp)
                        }
                    }
                    else {
                        convertExprNode(exp)
                    }
                }
                is PathComponentUnpivot  -> case {
                    meta(it) {
                        sexp {
                            symbol("*")
                            symbol("unpivot")
                        }
                    }
                }
                is PathComponentWildcard -> case {
                    meta(it) {
                        sexp {
                            symbol("*")
                        }
                    }
                }
            }.toUnit()
        }
    }

    private fun IonWriterContext.convertNAry(expr: NAry) {
        val (op, args, _: MetaContainer) = expr

        // The new AST has no equivalent for not_between, not_like and is_not.
        // These are expressed by wrapping between, like and is in an NAry with NAryOp.Not operation.
        // The true branch will unwrap that expression, preserving the original AST form.
        if(op == NAryOp.NOT && expr.metas.hasMeta(LegacyLogicalNotMeta.TAG)) {
            val firstArg = args.first()
            //Note: it is intentional that this is `when` statement and not an expression
            when (firstArg) {
                is NAry -> {
                    val (argOp, argArgs, _: MetaContainer) = firstArg

                    fun recurseArgs() {
                        argArgs.forEach { convertExprNode(it) }
                    }
                    when (argOp) {
                        NAryOp.BETWEEN -> {
                            symbol("not_between")
                            recurseArgs()
                        }
                        NAryOp.LIKE    -> {
                            symbol("not_like")
                            recurseArgs()
                        }
                        NAryOp.IN      -> {
                            symbol("not_in")
                            recurseArgs()
                        }
                        else           -> {
                            throw IllegalStateException("Invalid NAryOp on argument of `(not )` node decorated with LegacyLogicalNotMeta")
                        }
                    }
                }
                is Typed -> {
                    if(firstArg.op != TypedOp.IS) {
                        throw IllegalStateException("Invalid TypedOp on argument of `(not )` node decorated with LegacyLogicalNotMeta")
                    }
                    symbol("is_not")
                    convertExprNode(firstArg.expr)
                    convertDataType(firstArg.type)
                }
                else -> {
                    throw IllegalStateException("Invalid node type of of `(not )` node decorated with LegacyLogicalNotMeta")
                }

            }
            return
        }

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
                    convertExprNode(it)
                }
            }
            else -> {
                args.forEach {
                    convertExprNode(it)
                }
            }
        }
    }

    private fun ExprNode.getFuncName(): String {
        return when (this) {
            is VariableReference -> id
            else -> throw UnsupportedOperationException(
                "The V0 AST does not support calling arbitrary expressions, only " +
                "functions identified by name.")
        }
    }

    private fun convertDataType(dataType: DataType) {
        meta(dataType) {
            writerContext.sexp {
                symbol("type")
                symbol(dataType.sqlDataType.typeName)
                dataType.args.forEach {
                    int(it)
                }
            }
        }
    }
}