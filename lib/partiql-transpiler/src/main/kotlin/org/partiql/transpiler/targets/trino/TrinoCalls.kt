package org.partiql.transpiler.targets.trino

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.AstFactory
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.error
import org.partiql.transpiler.info
import org.partiql.transpiler.sql.SqlArgs
import org.partiql.transpiler.sql.SqlCallFn
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.types.BoolType
import org.partiql.types.IntType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

@OptIn(PartiQLValueExperimental::class)
public class TrinoCalls(private val onProblem: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
    }

    override fun eqFn(args: SqlArgs): Expr {
        val t0 = args[0].type
        val t1 = args[1].type
        if (!typesAreComparable(t0, t1)) {
            onProblem.error("Types $t0 and $t1 are not comparable in trino")
        }
        return super.eqFn(args)
    }

    private fun typesAreComparable(t0: StaticType, t1: StaticType): Boolean {
        if (t0 == t1 || t0.toString() == t1.toString()) {
            return true
        }
        if (t0 is BoolType && t1 is BoolType) {
            return true
        }
        if (t0 is IntType && t1 is IntType) {
            return true
        }
        return false
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_add
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val call = identifierSymbol("date_add", Identifier.CaseSensitivity.INSENSITIVE)
        onProblem.info("arg0 of date_add went from type `symbol` to `string`")
        val arg0 = exprLit(stringValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#date_diff
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val call = identifierSymbol("date_diff", Identifier.CaseSensitivity.INSENSITIVE)
        onProblem.info("arg0 of date_diff went from type `symbol` to `string`")
        val arg0 = exprLit(stringValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(call, listOf(arg0, arg1, arg2))
    }

    /**
     * https://trino.io/docs/current/functions/datetime.html#current_timestamp
     * https://trino.io/docs/current/functions/datetime.html#at_timezone
     *
     * at_timezone(current_timestamp, 'UTC')
     */
    private fun utcnow(args: SqlArgs): Expr = Ast.create {
        val call = id("at_timezone")
        onProblem.info("PartiQL `utcnow()` was replaced by Trino `at_timezone(current_timestamp, 'UTC')`")
        val arg0 = exprVar(id("current_timestamp"), Expr.Var.Scope.DEFAULT)
        val arg1 = exprLit(stringValue("UTC"))
        exprCall(call, listOf(arg0, arg1))
    }

    private fun AstFactory.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
