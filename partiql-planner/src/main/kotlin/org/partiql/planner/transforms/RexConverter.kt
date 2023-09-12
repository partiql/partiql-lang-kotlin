package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.ATTRIBUTES
import org.partiql.planner.Env
import org.partiql.planner.typer.toNonNullStaticType
import org.partiql.planner.typer.toStaticType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.int64Value
import org.partiql.value.nullValue
import org.partiql.value.symbolValue

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    private val factory = Plan

    private inline fun <T : PlanNode> transform(block: PlanFactory.() -> T): T = factory.block()

    internal fun apply(expr: Expr, context: Env): Rex = expr.accept(ToRex, context) // expr.toRex()

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, Env>() {

        override fun defaultReturn(node: AstNode, context: Env): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprLit(node: Expr.Lit, context: Env) = transform {
            val type = when (node.value.isNull) {
                true -> node.value.type.toStaticType()
                else -> node.value.type.toNonNullStaticType()
            }
            val op = rexOpLit(node.value)
            rex(type, op)
        }

        override fun visitExprVar(node: Expr.Var, context: Env) = transform {
            val type = (StaticType.ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, context: Env) = transform {
            val type = (StaticType.ANY)
            // Args
            val arg = node.expr.accept(ToRex, context)
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, context: Env) = transform {
            val type = (StaticType.ANY)
            // Args
            val lhs = node.lhs.accept(ToRex, context)
            val rhs = node.rhs.accept(ToRex, context)
            val args = listOf(lhs, rhs)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprPath(node: Expr.Path, context: Env): Rex = transform {
            val type = (StaticType.ANY)
            // Args
            val root = visitExpr(node.root, context)
            val steps = node.steps.map {
                when (it) {
                    is Expr.Path.Step.Index -> {
                        val key = visitExpr(it.key, context)
                        rexOpPathStepIndex(key)
                    }
                    is Expr.Path.Step.Symbol -> {
                        // Treat each symbol `foo` as ["foo"]
                        // Per resolution rules, we may be able to resolve and replace the first `n` symbols
                        val symbolType = (StaticType.SYMBOL)
                        val symbol = rexOpLit(symbolValue(it.symbol.symbol))
                        val key = rex(symbolType, symbol)
                        rexOpPathStepIndex(key)
                    }
                    is Expr.Path.Step.Unpivot -> rexOpPathStepUnpivot()
                    is Expr.Path.Step.Wildcard -> rexOpPathStepWildcard()
                }
            }
            // Rex
            val op = rexOpPath(root, steps)
            rex(type, op)
        }

        override fun visitExprCall(node: Expr.Call, context: Env) = transform {
            val type = (StaticType.ANY)
            // Args
            val args = node.args.map { visitExpr(it, context) }
            // Fn
            val id = AstToPlan.convert(node.function)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, context: Env) = transform {
            val type = (StaticType.ANY)
            val rex = when (node.expr) {
                null -> bool(true) // match `true`
                else -> visitExpr(node.expr!!, context) // match `rex
            }

            // Converts AST CASE (x) WHEN y THEN z --> Plan CASE WHEN x = y THEN z
            val id = identifierSymbol(Expr.Binary.Op.EQ.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            val createBranch: (Rex, Rex) -> Rex.Op.Case.Branch = { condition: Rex, result: Rex ->
                val op = rexOpCall(fn.copy(), listOf(rex, condition))
                val updatedCondition = rex(type, op)
                rexOpCaseBranch(updatedCondition, result)
            }

            val branches = node.branches.map {
                val branchCondition = visitExpr(it.condition, context)
                val branchRex = visitExpr(it.expr, context)
                createBranch(branchCondition, branchRex)
            }.toMutableList()

            val defaultRex = when (val default = node.default) {
                null -> rex(type = StaticType.NULL, op = rexOpLit(value = nullValue()))
                else -> visitExpr(default, context)
            }
            branches += rexOpCaseBranch(bool(true), defaultRex)
            val op = rexOpCase(branches)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, context: Env) = transform {
            val type = when (node.type) {
                Expr.Collection.Type.BAG -> StaticType.BAG
                Expr.Collection.Type.ARRAY -> StaticType.LIST
                Expr.Collection.Type.VALUES -> StaticType.LIST
                Expr.Collection.Type.LIST -> StaticType.LIST
                Expr.Collection.Type.SEXP -> StaticType.SEXP
            }
            val values = node.values.map { visitExpr(it, context) }
            val op = rexOpCollection(values)
            rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, context: Env) = transform {
            val type = (StaticType.STRUCT)
            val fields = node.fields.map {
                val k = visitExpr(it.name, context)
                val v = visitExpr(it.value, context)
                rexOpStructField(k, v)
            }
            val op = rexOpStruct(fields)
            rex(type, op)
        }

        // SPECIAL FORMS

        /**
         * <arg0> NOT? LIKE <arg1> ( ESCAPE <arg2>)?
         */
        override fun visitExprLike(node: Expr.Like, ctx: Env) = transform {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExpr(node.value, ctx)
            val arg1 = visitExpr(node.pattern, ctx)
            val arg2 = node.escape?.let { visitExpr(it, ctx) }
            // Call Variants
            var call = when (arg2) {
                null -> call("like", arg0, arg1)
                else -> call("like_escape", arg0, arg1, arg2)
            }
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            rex(type, call)
        }

        /**
         * <arg0> NOT? BETWEEN <arg1> AND <arg2>
         */
        override fun visitExprBetween(node: Expr.Between, ctx: Env) = transform {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExpr(node.value, ctx)
            val arg1 = visitExpr(node.from, ctx)
            val arg2 = visitExpr(node.to, ctx)
            // Call
            var call = call("between", arg0, arg1, arg2)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            rex(type, call)
        }

        /**
         * <arg0> NOT? IN <arg1>
         */
        override fun visitExprInCollection(node: Expr.InCollection, ctx: Env) = transform {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExpr(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx)
            // Call
            var call = call("in_collection", arg0, arg1)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            rex(type, call)
        }

        override fun visitExprIsType(node: Expr.IsType, ctx: Env) = transform {
            TODO("SQL Special Form is_type")
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Env): Rex = transform {
            val type = StaticType.ANY
            // Args
            val arg0 = rex(StaticType.LIST, rexOpCollection(node.args.map { visitExpr(it, ctx) }))
            // Call
            val call = call("coalesce", arg0)
            rex(type, call)
        }

        /**
         * NULLIF(<arg0>, <arg1>)
         */
        override fun visitExprNullIf(node: Expr.NullIf, ctx: Env) = transform {
            val type = StaticType.ANY
            // Args
            val arg0 = visitExpr(node.value, ctx)
            val arg1 = visitExpr(node.nullifier, ctx)
            // Call
            val call = call("null_if", arg0, arg1)
            rex(type, call)
        }

        /**
         * SUBSTRING(<arg0> (FROM <arg1> (FOR <arg2>)?)? )
         */
        override fun visitExprSubstring(node: Expr.Substring, ctx: Env) = transform {
            val type = StaticType.ANY
            // Args
            val arg0 = visitExpr(node.value, ctx)
            val arg1 = node.start?.let { visitExpr(it, ctx) } ?: rex(StaticType.INT, rexOpLit(int64Value(1)))
            val arg2 = node.length?.let { visitExpr(it, ctx) }
            // Call Variants
            val call = when (arg2) {
                null -> call("substring", arg0, arg1)
                else -> call("substring_length", arg0, arg1, arg2)
            }
            rex(type, call)
        }

        /**
         * POSITION(<arg0> IN <arg1>)
         */
        override fun visitExprPosition(node: Expr.Position, ctx: Env) = transform {
            val type = StaticType.ANY
            // Args
            val arg0 = visitExpr(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx)
            // Call
            val call = call("position", arg0, arg1)
            rex(type, call)
        }

        /**
         * TRIM([LEADING|TRAILING|BOTH]? (<arg1> FROM)? <arg0>)
         */
        override fun visitExprTrim(node: Expr.Trim, ctx: Env) = transform {
            val type = StaticType.TEXT
            // Args
            val arg0 = visitExpr(node.value, ctx)
            val arg1 = node.chars?.let { visitExpr(it, ctx) }
            // Call Variants
            val call = when (node.spec) {
                Expr.Trim.Spec.LEADING -> when (arg1) {
                    null -> call("trim_leading", arg0)
                    else -> call("trim_leading_chars", arg0, arg1)
                }
                Expr.Trim.Spec.TRAILING -> when (arg1) {
                    null -> call("trim_trailing", arg0)
                    else -> call("trim_trailing_chars", arg0, arg1)
                }
                else -> when (arg1) {
                    null -> call("trim", arg0)
                    else -> call("trim_chars", arg0, arg1)
                }
            }
            rex(type, call)
        }

        override fun visitExprOverlay(node: Expr.Overlay, ctx: Env): Rex {
            TODO("SQL Special Form OVERLAY")
        }

        override fun visitExprExtract(node: Expr.Extract, ctx: Env): Rex {
            TODO("SQL Special Form EXTRACT")
        }

        override fun visitExprCast(node: Expr.Cast, ctx: Env): Rex {
            TODO("SQL Special Form CAST")
        }

        override fun visitExprCanCast(node: Expr.CanCast, ctx: Env): Rex {
            TODO("PartiQL Special Form CAN_CAST")
        }

        override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Env): Rex {
            TODO("PartiQL Special Form CAN_LOSSLESS_CAST")
        }

        override fun visitExprDateAdd(node: Expr.DateAdd, ctx: Env) = transform {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = visitExpr(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_ADD(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_ADD(TIMEZONE_MINUTE, ...)")
                else -> call("date_add_${node.field.name.lowercase()}", arg0, arg1)
            }
            rex(type, call)
        }

        override fun visitExprDateDiff(node: Expr.DateDiff, ctx: Env) = transform {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = visitExpr(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_DIFF(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_DIFF(TIMEZONE_MINUTE, ...)")
                else -> call("date_diff_${node.field.name.lowercase()}", arg0, arg1)
            }
            rex(type, call)
        }

        override fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: Env) = transform {
            val type = StaticType.ANY
            val attribute = node.attribute.name.uppercase()
            val fn = ATTRIBUTES[attribute]
            if (fn == null) {
                // err?
                error("Unknown session attribute $attribute")
            }
            val call = call(fn)
            rex(type, call)
        }

        /**
         * This indicates we've hit a SQL `SELECT` subquery in the context of an expression tree.
         * There, coerce to scalar via COLL_TO_SCALAR: https://partiql.org/dql/subqueries.html#scalar-subquery
         *
         * The default behavior is to coerce, but we remove the scalar coercion in the special cases,
         *  - RHS of comparison when LHS is an array; and visa-versa
         *  - It is the collection expression of a FROM clause
         *  - It is the RHS of an IN predicate
         */
        override fun visitExprSFW(node: Expr.SFW, context: Env): Rex = transform {
            val query = RelConverter.apply(node, context)
            when (val select = query.op) {
                is Rex.Op.Select -> {
                    if (node.select is Select.Value) {
                        // SELECT VALUE does not implicitly coerce to a scalar
                        return query
                    }
                    // Insert the coercion
                    val type = select.constructor.type
                    val subquery = rexOpCollToScalarSubquery(select, query.type)
                    rex(type, rexOpCollToScalar(subquery))
                }
                else -> query
            }
        }

        // Helpers

        private fun bool(v: Boolean): Rex {
            val type = StaticType.BOOL
            val op = Plan.rexOpLit(boolValue(v))
            return Plan.rex(type, op)
        }

        private fun PlanFactory.negate(call: Rex.Op.Call): Rex.Op.Call {
            val name = Expr.Unary.Op.NOT.name
            val id = identifierSymbol(name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            // wrap
            val arg = rex(StaticType.BOOL, call)
            // rewrite call
            return rexOpCall(fn, listOf(arg))
        }

        private fun PlanFactory.call(name: String, vararg args: Rex): Rex.Op.Call {
            val id = identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            return rexOpCall(fn, args.toList())
        }
    }
}
