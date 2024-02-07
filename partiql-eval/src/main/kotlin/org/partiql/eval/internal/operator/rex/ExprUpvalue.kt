package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.util.Stack

/**
 * Returns the appropriate value from the stack.
 */
internal class ExprUpvalue(
    private val frameIndex: Int,
    private val varIndex: Int,
    private val scopes: Stack<Record>
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        return scopes[frameIndex][varIndex]
    }
}
