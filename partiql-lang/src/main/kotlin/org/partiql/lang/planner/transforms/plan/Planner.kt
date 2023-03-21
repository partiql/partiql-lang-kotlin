package org.partiql.lang.planner.transforms.plan

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.planner.transforms.normalize
import org.partiql.lang.syntax.Parser
import org.partiql.plan.Rex

object Planner {
    fun plan(query: String, parser: Parser): Rex {
        val ast = parser.parseAstStatement(query)
        val normalizedAST = ast.normalize() as? PartiqlAst.Statement.Query ?: error("Planner only support query for now.")
        return when (val expr = normalizedAST.expr) {
            is PartiqlAst.Expr.Select -> RelConverter.convert(expr)
            else -> RexConverter.convert(expr)
        }
    }
}
