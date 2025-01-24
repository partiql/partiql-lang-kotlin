package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.function.Fn
import org.partiql.spi.value.Datum

/**
 * Implementation of a scalar function call.
 *
 * @property function   Function instance to invoke.
 * @property args       Input argument expressions.
 */
internal class ExprCall(
    private var function: Fn,
    private var args: Array<ExprValue>,
) : ExprValue {

    private var isNullCall: Boolean = function.signature.isNullCall
    private var isMissingCall: Boolean = function.signature.isMissingCall
    private var nil = { Datum.nullValue(function.signature.returns) }
    private var missing = { Datum.missing(function.signature.returns) }

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
