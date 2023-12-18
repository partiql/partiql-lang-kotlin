package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.missingValue
import org.partiql.value.structValue

internal class ExprTupleUnion(
    val args: Array<Operator.Expr>
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        // Return MISSING on Mistyping Case
        val tuples = args.map {
            when (val arg = it.eval(record)) {
                is StructValue<*> -> arg
                is NullValue -> structValue(null)
                else -> when (arg.isNull) {
                    true -> structValue<PartiQLValue>(null)
                    false -> return missingValue()
                }
            }
        }

        // Return NULL if any arguments are NULL
        tuples.forEach {
            if (it.isNull) {
                return structValue<PartiQLValue>(null)
            }
        }

        return structValue(tuples.flatMap { it.entries })
    }
}
