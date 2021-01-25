package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.NullPropagatingExprFunction
import org.partiql.lang.eval.timestampValue
import java.math.BigDecimal

/**
 * Builtin function to convert the given PartiQL `TIMESTAMP` [ExprValue] into a unix epoch, where a unix epoch
 * represents the seconds since '1970-01-01 00:00:00' UTC. Largely based off MySQL's UNIX_TIMESTAMP.
 *
 * Syntax: `UNIX_TIMESTAMP([timestamp])`
 *
 * If UNIX_TIMESTAMP() is called with no [timestamp] argument, it returns the number of whole seconds since
 * '1970-01-01 00:00:00' UTC as a PartiQL `INT` [ExprValue]
 *
 * If UNIX_TIMESTAMP() is called with a [timestamp] argument, it returns the number of seconds from
 * '1970-01-01 00:00:00' UTC to the given [timestamp] argument. If given a [timestamp] before the last epoch, will
 * return the number of seconds before the last epoch as a negative number. The return value will be a decimal if and
 * only if the given [timestamp] has a fractional seconds part.
 *
 * The valid range of argument values is the range of PartiQL's `TIMESTAMP` value.
 */
internal class UnixTimestampFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("unix_timestamp", 0..1, valueFactory) {
    private val millisPerSecond = BigDecimal(1000)

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val timestamp = if (args.isEmpty()) {
            env.session.now
        } else {
            args[0].timestampValue()
        }

        val numMillis = timestamp.decimalMillis
        val epochTime = numMillis.divide(millisPerSecond)

        if (timestamp.decimalSecond.scale() == 0 || args.isEmpty()) {
            return valueFactory.newInt(epochTime.toLong())
        }

        return valueFactory.newDecimal(epochTime)
    }
}
