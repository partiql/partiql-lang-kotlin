package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.NullPropagatingExprFunction
import org.partiql.lang.eval.bigDecimalValue
import java.math.BigDecimal

/**
 * Builtin function to convert the given unix epoch into a PartiQL `TIMESTAMP` [ExprValue]. A unix epoch represents
 * the seconds since '1970-01-01 00:00:00' UTC. Largely based off MySQL's FROM_UNIXTIME.
 *
 * Syntax: `FROM_UNIXTIME(unix_timestamp)`
 * Where unix_timestamp is a (potentially decimal) numeric value. If unix_timestamp is a decimal, the returned
 * `TIMESTAMP` will have fractional seconds. If unix_timestamp is an integer, the returned `TIMESTAMP` will not have
 * fractional seconds.
 *
 * When given a negative numeric value, this function returns a PartiQL `TIMESTAMP` [ExprValue] before the last epoch.
 * When given a non-negative numeric value, this function returns a PartiQL `TIMESTAMP` [ExprValue] after the last
 * epoch.
 */
internal class FromUnixTimeFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("from_unixtime", 1, valueFactory) {
    private val millisPerSecond = BigDecimal(1000)

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val unixTimestamp = args[0].bigDecimalValue()

        val numMillis = unixTimestamp.times(millisPerSecond).stripTrailingZeros()

        val timestamp = Timestamp.forMillis(numMillis, null)
        return valueFactory.newTimestamp(timestamp)
    }
}
