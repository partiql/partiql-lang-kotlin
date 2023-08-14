package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.planner.Env
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.symbolValue

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr, context: Env): Rex = expr.accept(ToRex, context) // expr.toRex()

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, Env>() {

        override fun defaultReturn(node: AstNode, context: Env): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprLit(node: Expr.Lit, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            val op = rexOpLit(node.value)
            rex(type, op)
        }

        override fun visitExprVar(node: Expr.Var, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            // Args
            val arg = rexOpCallArgValue(node.expr.accept(ToRex, context))
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            // Args
            val lhs = rexOpCallArgValue(node.lhs.accept(ToRex, context))
            val rhs = rexOpCallArgValue(node.rhs.accept(ToRex, context))
            val args = listOf(lhs, rhs)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprPath(node: Expr.Path, context: Env): Rex = Plan.create {
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

        override fun visitExprCall(node: Expr.Call, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            // Args
            val args = node.args.map {
                val rex = visitExpr(it, context)
                rexOpCallArgValue(rex)
            }
            // Fn
            val id = AstToPlan.convert(node.function)
            val fn = fnUnresolved(id)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, context: Env) = Plan.create {
            val type = (StaticType.ANY)
            val rex = when (node.expr) {
                null -> context.bool(true) // match `true`
                else -> visitExpr(node.expr!!, context) // match `rex
            }
            val branches = node.branches.map {
                val branchCondition = visitExpr(it.condition, context)
                val branchRex = visitExpr(it.expr, context)
                rexOpCaseBranch(branchCondition, branchRex)
            }.toMutableList()
            if (node.default != null) {
                val defaultCondition = context.bool(true)
                val defaultRex = visitExpr(node.default!!, context)
                branches += rexOpCaseBranch(defaultCondition, defaultRex)
            }
            val op = rexOpCase(rex, branches)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, context: Env) = Plan.create {
            val t = when (node.type) {
                Expr.Collection.Type.BAG -> StaticType.BAG
                Expr.Collection.Type.ARRAY -> StaticType.LIST
                Expr.Collection.Type.VALUES -> StaticType.LIST
                Expr.Collection.Type.LIST -> StaticType.LIST
                Expr.Collection.Type.SEXP -> StaticType.SEXP
            }
            val type = (t)
            val values = node.values.map { visitExpr(it, context) }
            val op = rexOpCollection(values)
            rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, context: Env) = Plan.create {
            val type = (StaticType.STRUCT)
            val fields = node.fields.map {
                val k = visitExpr(it.name, context)
                val v = visitExpr(it.value, context)
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
        override fun visitExprSFW(node: Expr.SFW, context: Env): Rex = Plan.create {
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

        private fun Env.bool(v: Boolean): Rex {
            val type = StaticType.BOOL
            val op = Plan.rexOpLit(boolValue(v))
            return Plan.rex(type, op)
        }
    }
}
