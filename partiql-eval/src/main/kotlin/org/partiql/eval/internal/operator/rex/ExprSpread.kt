package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprSpread(
    val args: Array<Operator.Expr>
) : Operator.Expr {

    private val struct = PType.struct()

    override fun eval(): Datum {
        val tuples = args.map {
            it.eval().check(struct)
        }

        // Return NULL if any arguments are NULL
        tuples.forEach {
            if (it.isNull) {
                return Datum.nullValue(struct)
            }
        }

        return Datum.struct(tuples.flatMap { it.fields.asSequence() })
    }
}
