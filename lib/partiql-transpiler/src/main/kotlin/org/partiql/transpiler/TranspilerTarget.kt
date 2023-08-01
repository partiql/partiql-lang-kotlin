package org.partiql.transpiler

import com.amazon.ion.IonSymbol
import org.partiql.ast.Ast
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.builder.AstFactory
import org.partiql.plan.Arg
import org.partiql.plan.Case
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.Step
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.transpiler.block.BlockWriter
import org.partiql.transpiler.targets.toSFW
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.symbolValue

/**
 * A target determines the behavior of each stage of the transpilation.
 */
abstract class TranspilerTarget {
    abstract val target: String
    abstract val version: String
    abstract val dialect: Dialect

    /**
     * Default retarget is just a pass-through
     *
     * @param plan
     * @param onProblem
     */
    open fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback) = plan

    /**
     * Default AST to SQL
     *
     * @param statement
     * @param onProblem
     * @return
     */
    open fun unparse(statement: Statement, onProblem: ProblemCallback): String {
        val root = dialect.write(statement, onProblem)
        return BlockWriter.write(root)
    }

    /**
     * Default Plan to AST
     *
     * @param plan
     * @param onProblem
     * @return
     */
    open fun unplan(plan: PartiQLPlan, onProblem: ProblemCallback): Statement {
        val ruleset = BaseRuleset(onProblem)
        val expr = ruleset.visitRex(plan.root, Unit)
        return Ast.statementQuery(expr)
    }

    /**
     * PartiQLTarget.BaseRuleset transforms a query from the Plan domain to the AST domain.
     *
     * This is naive, but
     * straightforward. Also, this only targets simple SFW so we assume the Plan is simply: SCAN -> FILTER -> PROJECT.
     *
     * This will require non-trivial rework to handle arbitrary plans.
     *
     * It is recommended that this class is extended by TranspilerTarget implementations.
     *
     * @property onProblem  Invoked when a translation problem occurs
     */
    public open class BaseRuleset(val onProblem: ProblemCallback) : PlanBaseVisitor<AstNode, Unit>() {

        private val factory: AstFactory = Ast

        private inline fun <T : AstNode> unplan(block: AstFactory.() -> T): T = factory.block()

        override fun defaultReturn(node: PlanNode, ctx: Unit): AstNode =
            throw UnsupportedOperationException("Cannot unplan $node")

        override fun visitRex(node: Rex, ctx: Unit) = super.visitRex(node, ctx) as Expr

        override fun visitRexId(node: Rex.Id, ctx: Unit) = unplan {
            val identifier = id(node.name, node.case)
            val scope = when (node.qualifier) {
                Rex.Id.Qualifier.UNQUALIFIED -> Expr.Var.Scope.DEFAULT
                Rex.Id.Qualifier.LOCALS_FIRST -> Expr.Var.Scope.LOCAL
            }
            exprVar(identifier, scope)
        }

        override fun visitRexPath(node: Rex.Path, ctx: Unit) = unplan {
            val root = visitRex(node.root, ctx)
            val steps = node.steps.map { visitStep(it, ctx) }
            exprPath(root, steps)
        }

        override fun visitRexLit(node: Rex.Lit, ctx: Unit): AstNode {
            TODO()
        }

        override fun visitRexUnary(node: Rex.Unary, ctx: Unit) = unplan {
            // temporary
            val op = when (node.op) {
                Rex.Unary.Op.NOT -> Expr.Unary.Op.NOT
                Rex.Unary.Op.POS -> Expr.Unary.Op.POS
                Rex.Unary.Op.NEG -> Expr.Unary.Op.NEG
            }
            val expr = visitRex(node.value, ctx)
            exprUnary(op, expr)
        }

        override fun visitRexBinary(node: Rex.Binary, ctx: Unit) = unplan {
            // temporary, could do "valueOf" but that's fragile
            val op = when (node.op) {
                Rex.Binary.Op.PLUS -> Expr.Binary.Op.PLUS
                Rex.Binary.Op.MINUS -> Expr.Binary.Op.MINUS
                Rex.Binary.Op.TIMES -> Expr.Binary.Op.TIMES
                Rex.Binary.Op.DIV -> Expr.Binary.Op.DIVIDE
                Rex.Binary.Op.MODULO -> Expr.Binary.Op.MODULO
                Rex.Binary.Op.CONCAT -> Expr.Binary.Op.CONCAT
                Rex.Binary.Op.AND -> Expr.Binary.Op.AND
                Rex.Binary.Op.OR -> Expr.Binary.Op.OR
                Rex.Binary.Op.EQ -> Expr.Binary.Op.EQ
                Rex.Binary.Op.NEQ -> Expr.Binary.Op.NE
                Rex.Binary.Op.GTE -> Expr.Binary.Op.GTE
                Rex.Binary.Op.GT -> Expr.Binary.Op.GT
                Rex.Binary.Op.LT -> Expr.Binary.Op.LT
                Rex.Binary.Op.LTE -> Expr.Binary.Op.LTE
            }
            val lhs = visitRex(node.lhs, ctx)
            val rhs = visitRex(node.rhs, ctx)
            exprBinary(op, lhs, rhs)
        }

        override fun visitRexCall(node: Rex.Call, ctx: Unit) = unplan {
            // handle special functions
            when (node.id) {
                "like" -> callLike(node.args, ctx)
                "between" -> callBetween(node.args, ctx)
                "in_collection" -> callInCollection(node.args, ctx)
                "is_type" -> callIsType(node.args, ctx)
                "coalesce" -> callCoalesce(node.args, ctx)
                "null_if" -> callNullIf(node.args, ctx)
                "substring" -> callSubstring(node.args, ctx)
                "position" -> callPosition(node.args, ctx)
                "trim" -> callTrim(node.args, ctx)
                "overlay" -> callOverlay(node.args, ctx)
                "extract" -> callExtract(node.args, ctx)
                "cast" -> callCast(node.args, ctx)
                "can_lossless_cast" -> callCanLosslessCast(node.args, ctx)
                "date_add" -> callDateAdd(node.args, ctx)
                "date_diff" -> callDateDiff(node.args, ctx)
                "outer_union" -> callOuterUnion(node.args, ctx)
                "outer_union_distinct" -> callOuterUnionDistinct(node.args, ctx)
                "outer_intersect" -> callOuterIntersect(node.args, ctx)
                "outer_intersect_distinct" -> callOuterIntersectDistinct(node.args, ctx)
                "outer_except" -> callOuterExcept(node.args, ctx)
                "outer_except_distinct" -> callOuterExceptDistinct(node.args, ctx)
                else -> {
                    val id = identifierSymbol(node.id, Identifier.CaseSensitivity.INSENSITIVE)
                    val args = node.args.map {
                        when (it) {
                            is Arg.Type -> {
                                // record the error, return a symbol literal expression
                                onProblem(
                                    TranspilerProblem(
                                        level = TranspilerProblem.Level.ERROR,
                                        message = "Argument for call ${node.id} cannot be a type argument ${it.type}"
                                    )
                                )
                                it.type.toSymbolExpression()
                            }
                            is Arg.Value -> visitRex(it.value, ctx)
                        }
                    }
                    exprCall(id, args)
                }
            }
        }

        // TODO
        override fun visitRexSwitch(node: Rex.Switch, ctx: Unit) = defaultVisit(node, ctx)

        override fun visitRexAgg(node: Rex.Agg, ctx: Unit) = unplan {
            val fn = node.id.lowercase()
            val setq = when (node.modifier) {
                Rex.Agg.Modifier.ALL -> SetQuantifier.ALL
                Rex.Agg.Modifier.DISTINCT -> SetQuantifier.DISTINCT
            }
            when (fn) {
                "count_star" -> aggCallCountStar(node.args, setq, ctx)
                "count" -> aggCallCount(node.args, setq, ctx)
                "sum" -> aggCallSum(node.args, setq, ctx)
                "avg" -> aggCallAvg(node.args, setq, ctx)
                "min" -> aggCallMin(node.args, setq, ctx)
                "max" -> aggCallMax(node.args, setq, ctx)
                else -> {
                    val problem = TranspilerProblem(
                        level = TranspilerProblem.Level.ERROR,
                        message = "Unknown aggregation function $fn",
                    )
                    onProblem(problem)
                    // literal translation, no introspection
                    val identifier = id(fn, Case.INSENSITIVE)
                    val args = node.args.map { visitRex(it, ctx) }
                    exprAgg(identifier, args, null)
                }
            }
        }

        override fun visitRexCollection(node: Rex.Collection, ctx: Unit) =
            super.visitRexCollection(node, ctx) as Expr.Collection

        override fun visitRexCollectionArray(node: Rex.Collection.Array, ctx: Unit) = unplan {
            val type = Expr.Collection.Type.ARRAY
            val values = node.values.map { visitRex(it, ctx) }
            exprCollection(type, values)
        }

        override fun visitRexCollectionBag(node: Rex.Collection.Bag, ctx: Unit) = unplan {
            val type = Expr.Collection.Type.BAG
            val values = node.values.map { visitRex(it, ctx) }
            exprCollection(type, values)
        }

        override fun visitRexTuple(node: Rex.Tuple, ctx: Unit) = unplan {
            val fields = node.fields.map {
                val k = visitRex(it.name, ctx)
                val v = visitRex(it.value, ctx)
                exprStructField(k, v)
            }
            exprStruct(fields)
        }

        override fun visitRexQuery(node: Rex.Query, ctx: Unit) = super.visitRexQuery(node, ctx) as Expr.SFW

        override fun visitRexQueryScalarSubquery(node: Rex.Query.Scalar.Subquery, ctx: Unit) =
            visitRexQueryCollection(node.query, ctx)

        override fun visitRexQueryScalarPivot(node: Rex.Query.Scalar.Pivot, ctx: Unit) = unplan {
            val sfw = node.rel.toSFW(this@BaseRuleset)
            // Check that `select` has not been added
            if (sfw.select != null) {
                val problem = TranspilerProblem(
                    level = TranspilerProblem.Level.ERROR,
                    message = "Unsupported query plan. Plan has an SQL projection operator and a PIVOT projection",
                )
                onProblem(problem)
            }
            // Add the PIVOT Clause
            val key = visitRex(node.at, ctx)
            val value = visitRex(node.value, ctx)
            sfw.select = selectPivot(key, value)
            sfw.build()
        }

        override fun visitRexQueryCollection(node: Rex.Query.Collection, ctx: Unit) = unplan {
            val sfw = node.rel.toSFW(this@BaseRuleset)
            // ERR: Missing projection!
            if (node.constructor == null && sfw.select == null) {
                val problem = TranspilerProblem(
                    level = TranspilerProblem.Level.ERROR,
                    message = "Unsupported query plan; the input plan is missing either SELECT Clause or SELECT VALUE Clause",
                )
                onProblem(problem)
            }
            // ERR: Double projection!
            if (node.constructor != null && sfw.select == null) {
                val problem = TranspilerProblem(
                    level = TranspilerProblem.Level.ERROR,
                    message = "Unsupported query plan; the input plan suggests both SELECT and SELECT VALUE Clauses",
                )
                onProblem(problem)
            }
            if (node.constructor != null) {
                // SELECT VALUE
                val constructor = visitRex(node.constructor!!, Unit)
                val setq: SetQuantifier? = null // not yet supported
                sfw.select = selectValue(constructor, setq)
            }
            sfw.build()
        }

        // SQL and PartiQL Special Forms

        open fun callLike(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callBetween(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callInCollection(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callIsType(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callCoalesce(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callNullIf(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callSubstring(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callPosition(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callTrim(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOverlay(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callExtract(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callCast(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callCanLosslessCast(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callDateAdd(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callDateDiff(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterUnion(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterUnionDistinct(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterIntersect(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterIntersectDistinct(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterExcept(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        open fun callOuterExceptDistinct(args: List<Arg>, ctx: Unit): Expr = unplan { TODO() }

        // Aggregations

        open fun aggCallCountStar(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        open fun aggCallCount(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        open fun aggCallSum(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        open fun aggCallAvg(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        open fun aggCallMin(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        open fun aggCallMax(args: List<Rex>, setq: SetQuantifier, ctx: Unit): Expr = unplan { TODO() }

        // Other

        override fun visitStep(node: Step, ctx: Unit) = super.visitStep(node, ctx) as Expr.Path.Step

        override fun visitStepKey(node: Step.Key, ctx: Unit) = unplan {
            val k = node.value
            if (k is Rex.Lit && k.value is IonSymbol) {
                val identifier = id(k.value.toString(), node.case)
                exprPathStepSymbol(identifier)
            } else {
                val expr = visitRex(k, ctx)
                exprPathStepIndex(expr)
            }
        }

        override fun visitStepWildcard(node: Step.Wildcard, ctx: Unit) = unplan {
            exprPathStepWildcard()
        }

        override fun visitStepUnpivot(node: Step.Unpivot, ctx: Unit) = unplan {
            exprPathStepUnpivot()
        }

        // Helpers

        private fun AstFactory.id(name: String, case: Case): Identifier.Symbol = when (case) {
            Case.SENSITIVE -> identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE)
            Case.INSENSITIVE -> identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE)
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun StaticType.toSymbolExpression(): Expr.Lit = unplan {
            val symbol = when (this@toSymbolExpression) {
                is AnyOfType -> "ANY"
                is AnyType -> "ANY"
                is BlobType -> "BLOB"
                is BoolType -> "BOOL"
                is ClobType -> "CLOB"
                is BagType -> "BAG"
                is ListType -> "LIST"
                is SexpType -> "SEXP"
                is DateType -> "DATE"
                is DecimalType -> "DECIMAL"
                is FloatType -> "FLOAT"
                is GraphType -> "GRAPH"
                is IntType -> "INT"
                MissingType -> "MISSING"
                is NullType -> "NULL"
                is StringType -> "STRING"
                is StructType -> "STRUCT"
                is SymbolType -> "SYMBOL"
                is TimeType -> "TIME"
                is TimestampType -> "TIMESTAMP"
            }
            exprLit(symbolValue(symbol))
        }
    }
}
