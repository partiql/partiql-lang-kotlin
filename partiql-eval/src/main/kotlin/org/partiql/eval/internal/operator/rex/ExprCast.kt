package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class ExprCast(
    private val arg: Operator.Expr,
    private val cast: PType
) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val argDatum = arg.eval(env)
        return CastTable.cast(argDatum, cast)
    }
}
