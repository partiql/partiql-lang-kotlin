package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.toNull
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValueExperimental

@OptIn(FnExperimental::class, PartiQLValueExperimental::class)
internal class ExprCallStatic(
    private val fn: Fn,
    private val inputs: Array<Operator.Expr>,
) : Operator.Expr {

    /**
     * Memoize creation of nulls
     */
    private val nil = fn.signature.returns.toNull()

    override fun eval(env: Environment): Datum {
        // Evaluate arguments
        val args = inputs.map { input ->
            val r = input.eval(env)
            if (r.isNull && fn.signature.isNullCall) return Datum.of(nil())
            r.toPartiQLValue()
        }.toTypedArray()
        return Datum.of(fn.invoke(args))
    }
}
