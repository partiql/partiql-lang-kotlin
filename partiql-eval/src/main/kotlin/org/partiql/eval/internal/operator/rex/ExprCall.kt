package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum

/**
 * Implementation of a scalar function call.
 *
 * @property function   Function instance to invoke.
 * @property args       Input argument expressions.
 */
internal class ExprCall(
    private var function: Function.Instance,
    private var args: Array<Operator.Expr>,
) : Operator.Expr {

    private var isNullCall: Boolean = function.isNullCall
    private var isMissingCall: Boolean = function.isMissingCall
    private var nil = { Datum.nullValue(function.returns) }
    private var missing = { Datum.missing(function.returns) }

    override fun eval(env: Environment): Datum {
        // Evaluate arguments
        val args = Array(args.size) { i ->
            val arg = args[i].eval(env)
            if (isNullCall && arg.isNull) return nil()
            if (isMissingCall && arg.isMissing) return missing()
            arg
        }
        return function.invoke(args)
    }
}
