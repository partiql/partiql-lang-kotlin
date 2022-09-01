package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.bigDecimalValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
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
object FromUnixTime : ScalarFunction {
    override val signature = FunctionSignature(
        name = "from_unixtime",
        requiredParameters = listOf(listOf(DecimalType, IntType)),
        returnType = listOf(TimeStampType)
    )

    private val millisPerSecond = BigDecimal(1000)

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        val unixTimestamp = required[0].bigDecimalValue()

        val numMillis = unixTimestamp.times(millisPerSecond).stripTrailingZeros()

        val timestamp = Timestamp.forMillis(numMillis, null)
        return valueFactory.newTimestamp(timestamp)
    }
}
