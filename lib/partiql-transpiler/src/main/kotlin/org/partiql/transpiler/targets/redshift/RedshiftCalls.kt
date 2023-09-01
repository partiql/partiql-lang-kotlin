package org.partiql.transpiler.targets.redshift

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.AstFactory
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.info
import org.partiql.transpiler.sql.SqlArgs
import org.partiql.transpiler.sql.SqlCallFn
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class)
public class RedshiftCalls(private val onProblem: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = id("dateadd")
        onProblem.info("PartiQL `date_add` was replaced by Redshift `dateadd`")
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = id("datediff")
        onProblem.info("PartiQL `date_diff` was replaced by Redshift `datediff`")
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    private fun utcnow(args: SqlArgs): Expr = Ast.create {
        val id = id("sysdate")
        onProblem.info("PartiQL `utcnow()` was replaced by Redshift `SYSDATE`")
        exprVar(id, Expr.Var.Scope.DEFAULT)
    }

    private fun AstFactory.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
