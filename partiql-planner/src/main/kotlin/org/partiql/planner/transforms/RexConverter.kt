package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.PartiQLPlannerEnv
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.symbolValue

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr, env: PartiQLPlannerEnv): Rex = expr.accept(ToRex, env) // expr.toRex()

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, PartiQLPlannerEnv>() {

        private val factory: PlanFactory = Plan

        private inline fun transform(block: PlanFactory.() -> Rex): Rex = factory.block()

        override fun defaultReturn(node: AstNode, env: PartiQLPlannerEnv): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprLit(node: Expr.Lit, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            val op = rexOpLit(node.value)
            rex(type, op)
        }

        override fun visitExprVar(node: Expr.Var, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            // Args
            val arg = rexOpCallArgValue(node.expr.accept(ToRex, env))
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnRefUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            // Args
            val lhs = rexOpCallArgValue(node.lhs.accept(ToRex, env))
            val rhs = rexOpCallArgValue(node.rhs.accept(ToRex, env))
            val args = listOf(lhs, rhs)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnRefUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprPath(node: Expr.Path, env: PartiQLPlannerEnv): Rex = transform {
            val type = env.type(StaticType.ANY)
            // Args
            val root = visitExpr(node.root, env)
            val steps = node.steps.map {
                when (it) {
                    is Expr.Path.Step.Index -> {
                        val key = visitExpr(it.key, env)
                        rexOpPathStepIndex(key)
                    }
                    is Expr.Path.Step.Symbol -> {
                        // Treat each symbol `foo` as ["foo"]
                        // Per resolution rules, we may be able to resolve and replace the first `n` symbols
                        val symbolType = env.type(StaticType.SYMBOL)
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

        override fun visitExprCall(node: Expr.Call, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            // Args
            val args = node.args.map {
                val rex = visitExpr(it, env)
                rexOpCallArgValue(rex)
            }
            // Fn
            val id = AstToPlan.convert(node.function)
            val fn = fnRefUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.ANY)
            val rex = when (node.expr) {
                null -> env.bool(true)       // match `true`
                else -> visitExpr(node.expr!!, env) // match `rex
            }
            val branches = node.branches.map {
                val branchCondition = visitExpr(it.condition, env)
                val branchRex = visitExpr(it.expr, env)
                rexOpCaseBranch(branchCondition, branchRex)
            }.toMutableList()
            if (node.default != null) {
                val defaultCondition = env.bool(true)
                val defaultRex = visitExpr(node.default!!, env)
                branches += rexOpCaseBranch(defaultCondition, defaultRex)
            }
            val op = rexOpCase(rex, branches)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, env: PartiQLPlannerEnv) = transform {
            val t = when (node.type) {
                Expr.Collection.Type.BAG -> StaticType.BAG
                Expr.Collection.Type.ARRAY -> StaticType.LIST
                Expr.Collection.Type.VALUES -> StaticType.LIST
                Expr.Collection.Type.LIST -> StaticType.LIST
                Expr.Collection.Type.SEXP -> StaticType.SEXP
            }
            val type = env.type(t)
            val values = node.values.map { visitExpr(it, env) }
            val op = rexOpCollection(values)
            rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, env: PartiQLPlannerEnv) = transform {
            val type = env.type(StaticType.STRUCT)
            val fields = node.fields.map {
                val k = visitExpr(it.name, env)
                val v = visitExpr(it.value, env)
                rexOpStructField(k, v)
            }
            val op = rexOpStruct(fields)
            rex(type, op)
        }

        // TODO SPECIAL FORMS ONCE WE HAVE THE CATALOG !!

        /**
         * This indicates we've hit a SQL `SELECT` subquery in the context of an expression tree.
         * There, coerce to scalar via COLL_TO_SCALAR: https://partiql.org/dql/subqueries.html#scalar-subquery
         *
         * The default behavior is to coerce, but we remove the scalar coercion in the special cases,
         *  - RHS of comparison when LHS is an array; and visa-versa
         *  - It is the collection expression of a FROM clause
         *  - It is the RHS of an IN predicate
         */
        override fun visitExprSFW(node: Expr.SFW, env: PartiQLPlannerEnv): Rex = transform {
            val query = RelConverter.apply(node, env)
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

        private fun PartiQLPlannerEnv.type(type: StaticType) = resolveType(AstToPlan.convert(type))

        private fun PartiQLPlannerEnv.bool(v: Boolean): Rex {
            val type = type(StaticType.BOOL)
            val op = factory.rexOpLit(boolValue(v))
            return factory.rex(type, op)
        }
    }
}
