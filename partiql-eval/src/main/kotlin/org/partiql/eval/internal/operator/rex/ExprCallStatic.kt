package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.spi.function.Function
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprCallStatic(
    private val fn: Function,
    private val inputs: Array<Operator.Expr>,
) : Operator.Expr {

    /**
     * Memoize creation of nulls
     */
    private val nil = { Datum.nullValue(fn.signature.returns) }

    /**
     * Memoize creation of missing values
     */
    private val missing = { Datum.missing(fn.signature.returns) }

    override fun eval(env: Environment): Datum {
        // Evaluate arguments
        val args = inputs.map { input ->
            val r = input.eval(env)
            if (r.isNull && fn.signature.isNullCall) return nil.invoke()
            if (r.isMissing && fn.signature.isMissingCall) return missing.invoke()
            r
        }.toTypedArray()
        return fn.invoke(args)
    }
}
