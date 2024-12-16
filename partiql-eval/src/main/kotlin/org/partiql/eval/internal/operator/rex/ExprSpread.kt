package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprSpread(
    val args: Array<ExprValue>
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val tuples = args.map {
            it.eval(env).check(PType.struct())
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
