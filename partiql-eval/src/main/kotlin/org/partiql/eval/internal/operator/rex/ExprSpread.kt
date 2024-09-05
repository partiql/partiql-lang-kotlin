package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprSpread(
    val args: Array<Operator.Expr>
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): Datum {
        val tuples = args.map {
            it.eval(env).check(PartiQLValueType.STRUCT)
        }

        // Return NULL if any arguments are NULL
        tuples.forEach {
            if (it.isNull) {
                return Datum.nullValue(PType.struct())
            }
        }

        return Datum.struct(tuples.flatMap { it.fields.asSequence() })
    }
}
