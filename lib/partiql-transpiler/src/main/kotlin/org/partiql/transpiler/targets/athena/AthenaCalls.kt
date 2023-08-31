package org.partiql.transpiler.targets.athena

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.AstFactory
import org.partiql.transpiler.sql.SqlArgs
import org.partiql.transpiler.sql.SqlCallFn
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class)
public class AthenaCalls : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = identifierSymbol("dateadd", Identifier.CaseSensitivity.INSENSITIVE)
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = identifierSymbol("datediff", Identifier.CaseSensitivity.INSENSITIVE)
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    private fun utcnow(args: SqlArgs): Expr = Ast.create {
        val id = id("SYSDATE")
        exprVar(id, Expr.Var.Scope.DEFAULT)
    }

    private fun AstFactory.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
