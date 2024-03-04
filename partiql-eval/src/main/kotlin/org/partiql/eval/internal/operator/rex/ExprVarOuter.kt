package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Returns the appropriate value from the stack.
 */
internal class ExprVarOuter(
    private val depth: Int,
    private val reference: Int,
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): PartiQLValue {
        var current = env
        repeat(depth) {
            current = current.next() ?: error("We ran out of environments for depth ($depth) and env: $env.")
        }
        return current.getOrNull(reference) ?: error("The env doesn't have a variable for depth/ref ($depth/$reference) and env: $env. Current is: $current.")
    }
}
