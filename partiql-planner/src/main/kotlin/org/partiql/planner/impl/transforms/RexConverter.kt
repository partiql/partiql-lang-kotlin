package org.partiql.planner.impl.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.Env
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    public fun apply(expr: Expr, env: Env): Rex = expr.accept(ToRex, env) // expr.toRex()

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, Env>() {

        private val factory: PlanFactory = Plan

        private inline fun transform(block: PlanFactory.() -> Rex): Rex = factory.block()

        override fun defaultReturn(node: AstNode, env: Env): Rex = throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprLit(node: Expr.Lit, env: Env) = transform {
            val type = StaticType.ANY
            val op = rexOpLit(node.value)
            rex(type, op)
        }

        override fun visitExprVar(node: Expr.Var, env: Env) = transform {
            val type = StaticType.ANY
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, env: Env) = transform {
            val type = StaticType.ANY
            // Args
            val arg = node.expr.accept(ToRex, env)
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val inputs = listOf(arg.type)
            val fn = fn(id, inputs, type)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, env: Env) = transform {
            val type = StaticType.ANY
            // Args
            val lhs = node.lhs.accept(ToRex, env)
            val rhs = node.rhs.accept(ToRex, env)
            val args = listOf(lhs, rhs)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val inputs = args.map { it.type }
            val fn = fn(id, inputs, type)
            // Rex
            val op = rexOpCall(fn, args)
            rex(type, op)
        }
    }
}
