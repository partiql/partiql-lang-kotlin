package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
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
    private val nil = { Datum.nullValue(fn.signature.returns) }

    override fun eval(env: Environment): Datum {
        // Evaluate arguments
        val args = inputs.map { input ->
            val r = input.eval(env)
            if (r.isNull && fn.signature.isNullCall) return nil.invoke()
            if (r.isMissing && fn.signature.isMissingCall) throw TypeCheckException()
            r.toPartiQLValue()
        }.toTypedArray()
        return Datum.of(fn.invoke(args))
    }
}
