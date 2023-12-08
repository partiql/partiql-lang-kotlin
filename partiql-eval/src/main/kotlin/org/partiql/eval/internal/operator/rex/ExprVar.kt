package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Returns the value in the given record index.
 *
 * @property index
 */
internal class ExprVar(private val index: Int) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        return record.values[index]
    }
}
