package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.toNull
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal class ExprCallStatic(
    private val fn: PartiQLFunction.Scalar,
    private val inputs: Array<Operator.Expr>,
) : Operator.Expr {

    /**
     * Memoize creation of
     */
    @OptIn(PartiQLValueExperimental::class)
    private val nil = fn.signature.returns.toNull()

    override fun eval(record: Record): PartiQLValue {
        // Evaluate arguments
        val args = inputs.map { input ->
            val r = input.eval(record)
            if (r.isNull && fn.signature.isNullCall) return nil()
            r
        }.toTypedArray()
        return fn.invoke(args)
    }
}
