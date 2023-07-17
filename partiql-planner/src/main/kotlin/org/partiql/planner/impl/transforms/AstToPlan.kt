package org.partiql.planner.impl.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Type
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.impl.PartiQLPlannerEnv
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
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Identifier as PlanIdentifier
import org.partiql.plan.Statement as PlanStatement

internal object AstToPlan {

    // statement.toPlan()
    @JvmStatic
    fun apply(statement: AstStatement, env: PartiQLPlannerEnv): PlanStatement = statement.accept(ToPlanStatement, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToPlanStatement : AstBaseVisitor<PlanStatement, PartiQLPlannerEnv>() {

        private val factory = Plan

        private inline fun <T : PlanNode> transform(block: PlanFactory.() -> T): T = factory.block()

        override fun defaultReturn(node: AstNode, env: PartiQLPlannerEnv) = throw IllegalArgumentException("Unsupported statement")

        override fun visitStatementQuery(node: AstStatement.Query, env: PartiQLPlannerEnv) = transform {
            val rex = when (val expr = node.expr) {
                is Expr.SFW -> RelConverter.apply(expr, env)
                else -> RexConverter.apply(expr, env)
            }
            statementQuery(rex)
        }
    }

    // --- Helpers --------------------

    fun convert(identifier: AstIdentifier): PlanIdentifier = when (identifier) {
        is AstIdentifier.Qualified -> convert(identifier)
        is AstIdentifier.Symbol -> convert(identifier)
    }

    fun convert(identifier: AstIdentifier.Qualified): PlanIdentifier.Qualified {
        val root = convert(identifier.root)
        val steps = identifier.steps.map { convert(it) }
        return Plan.identifierQualified(root, steps)
    }

    fun convert(identifier: AstIdentifier.Symbol): PlanIdentifier.Symbol {
        val symbol = identifier.symbol
        val case = when (identifier.caseSensitivity) {
            AstIdentifier.CaseSensitivity.SENSITIVE -> PlanIdentifier.CaseSensitivity.SENSITIVE
            AstIdentifier.CaseSensitivity.INSENSITIVE -> PlanIdentifier.CaseSensitivity.INSENSITIVE
        }
        return Plan.identifierSymbol(symbol, case)
    }

    fun convert(type: StaticType): Type.Atomic {
        val symbol = when (type) {
            is AnyOfType -> "any"
            is AnyType -> "any"
            is BlobType -> "blob"
            is BoolType -> "bool"
            is ClobType -> "clob"
            is BagType -> "bag"
            is ListType -> "list"
            is SexpType -> "sexp"
            is DateType -> "date"
            is DecimalType -> "decimal"
            is FloatType -> "float"
            is GraphType -> "graph"
            is IntType -> "int"
            MissingType -> "missing"
            is NullType -> "null"
            is StringType -> "string"
            is StructType -> "struct"
            is SymbolType -> "symbol"
            is TimeType -> "time"
            is TimestampType -> "timestamp"
        }
        return Plan.typeAtomic(symbol)
    }
}
