package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprSpread(
    val args: Array<Expression>
) : Expression {

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
