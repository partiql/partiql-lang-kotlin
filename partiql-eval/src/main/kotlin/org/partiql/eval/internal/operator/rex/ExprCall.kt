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
        val args = Array(args.size) { i -> args[i].eval(env) }
        if (isMissingCall && args.any { it.isMissing }) return missing()
        if (isNullCall && args.any { it.isNull }) return nil()
        return function.invoke(args)
    }
}
