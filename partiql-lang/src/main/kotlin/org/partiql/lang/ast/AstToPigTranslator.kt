package org.partiql.lang.ast

import com.amazon.ionelement.api.ionSymbol
import org.partiql.ast.AstNode
import org.partiql.ast.Case
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Let
import org.partiql.ast.OrderBy
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.lang.domains.PartiqlAst
import org.partiql.parser.PartiQLParser
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Several PIG calls have special forms we need to recreate
 */
private typealias SpecialForm = (exprs: List<PartiqlAst.Expr>) -> Pair<String, List<Any>>

/**
 * Translates an [AstNode] tree to the legacy PIG AST
 */
object AstToPigTranslator {

    /**
     * Translates an [AstNode] tree to the legacy PIG AST
     */
    fun translate(ast: AstNode, locations: PartiQLParser.SourceLocations? = null): PartiqlAst.PartiqlAstNode {
        val node = ast.accept(Visitor, Ctx(locations))
        return node
    }

    /**
     * Undo lowering
     */
    private val specialCalls: Map<String, SpecialForm> = mapOf(
        "trim_whitespace_both" to { args -> "trim" to listOf("both", args[0]) },
        "trim_whitespace_leading" to { args -> "trim" to listOf("leading", args[0]) },
        "trim_whitespace_trailing" to { args -> "trim" to listOf("trailing", args[0]) },
        "trim_chars_both" to { args -> "trim" to listOf(args[0], "both", args[1]) },
        "trim_chars_leading" to { args -> "trim" to listOf(args[0], "leading", args[1]) },
        "trim_chars_trailing" to { args -> "trim" to listOf(args[0], "trailing", args[1]) },
    )

    /**
     * Visitor method arguments
     */
    private class Ctx(val locations: PartiQLParser.SourceLocations?)

    private object Visitor : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {

        private val factory = PartiqlAst.BUILDER()

        private inline fun <T : PartiqlAst.PartiqlAstNode> translate(
            node: AstNode,
            ctx: Ctx,
            block: PartiqlAst.Builder.() -> T,
        ): T {
            val piggy = factory.block()
            val location = when (val l = ctx.locations?.get(node.id)) {
                null -> UNKNOWN_SOURCE_LOCATION
                else -> SourceLocationMeta(l.line.toLong(), l.offset.toLong(), l.length.toLong())
            }
            @Suppress("UNCHECKED_CAST") return piggy.withMeta(SourceLocationMeta.TAG, location) as T
        }

        override fun visitExpr(node: Expr, ctx: Ctx) = super.visitExpr(node, ctx) as PartiqlAst.Expr

        override fun visitStatementQuery(node: Statement.Query, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            query(expr)
        }

        override fun visitType(node: Type, ctx: Ctx) = translate(node, ctx) {
            // parameters are only integers for now
            val parameters = node.parameters.map { it.asAnyElement().longValue }
            when (node.identifier) {
                "null" -> nullType()
                "missing" -> missingType()
                "any" -> anyType()
                "blob" -> blobType()
                "bool" -> booleanType()
                "bag" -> bagType()
                "array" -> listType()
                "sexp" -> sexpType()
                "date" -> dateType()
                "time" -> timeType()
                "timestamp" -> timestampType()
                "numeric" -> {
                    when (parameters.size) {
                        0 -> numericType()
                        1 -> numericType(parameters[0])
                        2 -> numericType(parameters[0], parameters[1])
                        else -> throw IllegalArgumentException("Too many parameters for numeric type")
                    }
                }
                "decimal" -> {
                    when (parameters.size) {
                        0 -> decimalType()
                        1 -> decimalType(parameters[0])
                        2 -> decimalType(parameters[0], parameters[1])
                        else -> throw IllegalArgumentException("Too many parameters for decimal type")
                    }
                }
                "float" -> floatType()
                "int" -> integerType()
                "varchar" -> {
                    if (parameters.isNotEmpty()) {
                        characterVaryingType(parameters[0])
                    } else {
                        characterVaryingType()
                    }
                }
                "tuple" -> structType()
                else -> throw IllegalArgumentException("Type ${node.identifier} does not exist in the PIG AST")
            }
        }

        override fun visitExprMissing(node: Expr.Missing, ctx: Ctx) = translate(node, ctx) {
            missing()
        }

        override fun visitExprLit(node: Expr.Lit, ctx: Ctx) = translate(node, ctx) {
            lit(node.value)
        }

        override fun visitExprIdentifier(node: Expr.Identifier, ctx: Ctx) = translate(node, ctx) {
            val case = when (node.case) {
                Case.SENSITIVE -> caseSensitive()
                Case.INSENSITIVE -> caseInsensitive()
            }
            val qualifier = when (node.scope) {
                Expr.Identifier.Scope.UNQUALIFIED -> unqualified()
                Expr.Identifier.Scope.LOCALS_FIRST -> localsFirst()
            }
            id(node.name, case, qualifier)
        }

        override fun visitExprPath(node: Expr.Path, ctx: Ctx) = translate(node, ctx) {
            val root = visitExpr(node.root, ctx)
            val steps = translate(node.steps, ctx, PartiqlAst.PathStep::class)
            path(root, steps)
        }

        override fun visitExprPathStep(node: Expr.Path.Step, ctx: Ctx) =
            super.visitExprPathStep(node, ctx) as PartiqlAst.PathStep

        override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: Ctx) = translate(node, ctx) {
            val key = visitExpr(node.key, ctx)
            val case = when (node.case) {
                Case.SENSITIVE -> caseSensitive()
                Case.INSENSITIVE -> caseInsensitive()
            }
            pathExpr(key, case)
        }

        override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: Ctx) =
            translate(node, ctx) {
                pathWildcard()
            }

        override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: Ctx) = translate(node, ctx) {
            pathUnpivot()
        }

        override fun visitExprCall(node: Expr.Call, ctx: Ctx) = translate(node, ctx) {
            val funcName = node.function
            val args = translate(node.args, ctx, PartiqlAst.Expr::class)
            when (val form = specialCalls[funcName]) {
                null -> call(funcName, args)
                else -> {
                    val rewriter = form(args)
                    val args = rewriter.second.map {
                        when (it) {
                            is PartiqlAst.Expr -> it
                            is String -> lit(ionSymbol(it))
                            else -> throw IllegalArgumentException("")
                        }
                    }
                    call(rewriter.first, args)
                }
            }
        }

        override fun visitExprAgg(node: Expr.Agg, ctx: Ctx) = translate(node, ctx) {
            val setq = translate(node.quantifier)
            val funcName = node.function
            val arg = visitExpr(node.args[0], ctx) // PIG callAgg only has one arg
            callAgg(setq, funcName, arg)
        }

        override fun visitExprParameter(node: Expr.Parameter, ctx: Ctx) = translate(node, ctx) {
            parameter(node.index.toLong())
        }

        override fun visitExprUnary(node: Expr.Unary, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            when (node.op) {
                Expr.Unary.Op.NOT -> not(expr)
                Expr.Unary.Op.POS -> pos(expr)
                Expr.Unary.Op.NEG -> neg(expr)
            }
        }

        override fun visitExprBinary(node: Expr.Binary, ctx: Ctx) = translate(node, ctx) {
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            when (node.op) {
                Expr.Binary.Op.PLUS -> plus(operands)
                Expr.Binary.Op.MINUS -> minus(operands)
                Expr.Binary.Op.TIMES -> times(operands)
                Expr.Binary.Op.DIVIDE -> divide(operands)
                Expr.Binary.Op.MODULO -> modulo(operands)
                Expr.Binary.Op.CONCAT -> concat(operands)
                Expr.Binary.Op.AND -> and(operands)
                Expr.Binary.Op.OR -> or(operands)
                Expr.Binary.Op.EQ -> eq(operands)
                Expr.Binary.Op.NE -> ne(operands)
                Expr.Binary.Op.GT -> gt(operands)
                Expr.Binary.Op.GTE -> gte(operands)
                Expr.Binary.Op.LT -> lt(operands)
                Expr.Binary.Op.LTE -> lte(operands)
            }
        }

        override fun visitExprCollection(node: Expr.Collection, ctx: Ctx) = translate(node, ctx) {
            val values = translate(node.values, ctx, PartiqlAst.Expr::class)
            when (node.type) {
                Expr.Collection.Type.BAG -> bag(values)
                Expr.Collection.Type.ARRAY -> list(values)
                Expr.Collection.Type.LIST -> list(values).withMeta(IsListParenthesizedMeta.tag, IsListParenthesizedMeta)
                Expr.Collection.Type.SEXP -> sexp(values)
            }
        }

        override fun visitExprTuple(node: Expr.Tuple, ctx: Ctx) = translate(node, ctx) {
            val fields = translate(node.fields, ctx, PartiqlAst.ExprPair::class)
            struct(fields)
        }

        override fun visitExprTupleField(node: Expr.Tuple.Field, ctx: Ctx) = translate(node, ctx) {
            val first = visitExpr(node.name, ctx)
            val second = visitExpr(node.value, ctx)
            exprPair(first, second)
        }

        override fun visitExprDate(node: Expr.Date, ctx: Ctx) = translate(node, ctx) {
            date(node.year, node.month, node.day)
        }

        override fun visitExprTime(node: Expr.Time, ctx: Ctx) = translate(node, ctx) {
            timeValue(
                node.hour,
                node.minute,
                node.second,
                node.nano,
                node.precision,
                node.tzOffsetMinutes != null,
                node.tzOffsetMinutes
            )
        }

        override fun visitExprLike(node: Expr.Like, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val pattern = visitExpr(node.pattern, ctx)
            val escape = node.escape?.let { visitExpr(it, ctx) }
            like(value, pattern, escape)
        }

        override fun visitExprBetween(node: Expr.Between, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val from = visitExpr(node.from, ctx)
            val to = visitExpr(node.to, ctx)
            between(value, from, to)
        }

        override fun visitExprInCollection(node: Expr.InCollection, ctx: Ctx) = translate(node, ctx) {
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            inCollection(operands)
        }

        override fun visitExprIsType(node: Expr.IsType, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val type = visitType(node.type, ctx)
            isType(value, type)
        }

        override fun visitExprSwitch(node: Expr.Switch, ctx: Ctx) = translate(node, ctx) {
            val pairs = exprPairList(translate(node.branches, ctx, PartiqlAst.ExprPair::class))
            val default = node.default?.let { visitExpr(it, ctx) }
            when (node.expr) {
                null -> searchedCase(pairs, default)
                else -> simpleCase(visitExpr(node.expr!!, ctx), pairs, default)
            }
        }

        override fun visitExprSwitchBranch(node: Expr.Switch.Branch, ctx: Ctx) = translate(node, ctx) {
            val first = visitExpr(node.condition, ctx)
            val second = visitExpr(node.expr, ctx)
            exprPair(first, second)
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Ctx) = translate(node, ctx) {
            val args = translate(node.args, ctx, PartiqlAst.Expr::class)
            coalesce(args)
        }

        override fun visitExprNullIf(node: Expr.NullIf, ctx: Ctx) = translate(node, ctx) {
            val expr1 = visitExpr(node.expr0, ctx)
            val expr2 = visitExpr(node.expr1, ctx)
            nullIf(expr1, expr2)
        }

        override fun visitExprCast(node: Expr.Cast, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            cast(value, asType)
        }

        override fun visitExprCanCast(node: Expr.CanCast, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            canCast(value, asType)
        }

        override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            canLosslessCast(value, asType)
        }

        override fun visitExprSet(node: Expr.Set, ctx: Ctx) = translate(node, ctx) {
            val quantifier = translate(node.quantifier)
            val op = when (node.op) {
                Expr.Set.Op.UNION -> if (node.outer) outerUnion() else union()
                Expr.Set.Op.INTERSECT -> if (node.outer) outerIntersect() else intersect()
                Expr.Set.Op.EXCEPT -> if (node.outer) outerExcept() else except()
            }
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            bagOp(op, quantifier, operands)
        }

        override fun visitExprSFW(node: Expr.SFW, ctx: Ctx) = translate(node, ctx) {
            var setq = when (val s = node.select) {
                is Select.Pivot -> null
                is Select.Project -> translate(s.quantifier)
                is Select.Star -> null
                is Select.Value -> translate(s.quantifier)
            }
            // PIG AST omits setq if ALL
            if (setq is PartiqlAst.SetQuantifier.All) {
                setq = null
            }
            val project = visitSelect(node.select, ctx)
            val from = visitFrom(node.from, ctx)
            val fromLet = node.let?.let { visitLet(it, ctx) }
            val where = node.where?.let { visitExpr(it, ctx) }
            val groupBy = node.groupBy?.let { visitGroupBy(it, ctx) }
            val having = node.having?.let { visitExpr(it, ctx) }
            val orderBy = node.orderBy?.let { visitOrderBy(it, ctx) }
            val limit = node.limit?.let { visitExpr(it, ctx) }
            val offset = node.offset?.let { visitExpr(it, ctx) }
            select(setq, project, from, fromLet, where, groupBy, having, orderBy, limit, offset)
        }

        override fun visitExprMatch(node: Expr.Match, ctx: Ctx) = translate(node, ctx) {
            TODO("GPML Translation not implemented")
        }

        override fun visitExprWindow(node: Expr.Window, ctx: Ctx) = translate(node, ctx) {
            TODO("WINDOW Translation not implemented")
        }

        override fun visitSelect(node: Select, ctx: Ctx) = super.visitSelect(node, ctx) as PartiqlAst.Projection

        override fun visitSelectStar(node: Select.Star, ctx: Ctx) = translate(node, ctx) {
            projectStar()
        }

        override fun visitSelectProject(node: Select.Project, ctx: Ctx) = translate(node, ctx) {
            val items = translate(node.items, ctx, PartiqlAst.ProjectItem::class)
            projectList(items)
        }

        override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            projectAll(expr)
        }

        override fun visitSelectProjectItemVar(node: Select.Project.Item.Var, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            projectExpr(expr, asAlias)
        }

        override fun visitSelectPivot(node: Select.Pivot, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.value, ctx)
            val key = visitExpr(node.key, ctx)
            projectPivot(value, key)
        }

        override fun visitSelectValue(node: Select.Value, ctx: Ctx) = translate(node, ctx) {
            val value = visitExpr(node.constructor, ctx)
            projectValue(value)
        }

        override fun visitFrom(node: From, ctx: Ctx) = super.visitFrom(node, ctx) as PartiqlAst.FromSource

        override fun visitFromCollection(node: From.Collection, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            val atAlias = node.atAlias
            val byAlias = node.byAlias
            when (node.unpivot) {
                true -> unpivot(expr, asAlias, atAlias, byAlias)
                else -> scan(expr, asAlias, atAlias, byAlias)
            }
        }

        override fun visitFromJoin(node: From.Join, ctx: Ctx) = translate(node, ctx) {
            val type = when (node.type) {
                From.Join.Type.INNER -> inner()
                From.Join.Type.LEFT -> left()
                From.Join.Type.RIGHT -> right()
                From.Join.Type.FULL -> full()
            }
            val left = visitFrom(node.lhs, ctx)
            val right = visitFrom(node.rhs, ctx)
            val predicate = node.condition?.let { visitExpr(it, ctx) }
            join(type, left, right, predicate)
        }

        override fun visitLet(node: Let, ctx: Ctx) = translate(node, ctx) {
            val bindings = translate(node.bindings, ctx, PartiqlAst.LetBinding::class)
            let(bindings)
        }

        override fun visitLetBinding(node: Let.Binding, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            val name = node.asAlias
            letBinding(expr, name)
        }

        override fun visitGroupBy(node: GroupBy, ctx: Ctx) = translate(node, ctx) {
            val strategy = when (node.strategy) {
                GroupBy.Strategy.FULL -> groupFull()
                GroupBy.Strategy.PARTIAL -> groupPartial()
            }
            val keyList = groupKeyList(translate(node.keys, ctx, PartiqlAst.GroupKey::class))
            val groupAsAlias = node.asAlias
            groupBy(strategy, keyList, groupAsAlias)
        }

        override fun visitGroupByKey(node: GroupBy.Key, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            groupKey(expr, asAlias)
        }

        override fun visitOrderBy(node: OrderBy, ctx: Ctx) = translate(node, ctx) {
            val sortSpecs = translate(node.sorts, ctx, PartiqlAst.SortSpec::class)
            orderBy(sortSpecs)
        }

        override fun visitOrderBySort(node: OrderBy.Sort, ctx: Ctx) = translate(node, ctx) {
            val expr = visitExpr(node.expr, ctx)
            val orderingSpec = when (node.dir) {
                OrderBy.Sort.Dir.ASC -> asc()
                OrderBy.Sort.Dir.DESC -> desc()
            }
            val nullsSpec = when (node.nulls) {
                OrderBy.Sort.Nulls.FIRST -> nullsFirst()
                OrderBy.Sort.Nulls.LAST -> nullsLast()
            }
            sortSpec(expr, orderingSpec, nullsSpec)
        }

        override fun defaultReturn(node: AstNode, ctx: Ctx) = translate(node, ctx) {
            TODO("Not yet implemented")
        }

        // -----

        private fun <T : PartiqlAst.PartiqlAstNode> translate(
            nodes: List<AstNode>,
            ctx: Ctx,
            clazz: KClass<T>
        ): List<T> {
            return nodes.map { clazz.cast(visit(it, ctx)) }
        }

        private fun translate(quantifier: SetQuantifier) = when (quantifier) {
            SetQuantifier.ALL -> PartiqlAst.SetQuantifier.All()
            SetQuantifier.DISTINCT -> PartiqlAst.SetQuantifier.Distinct()
        }
    }
}
