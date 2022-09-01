package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
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
data class UnixTimestamp(
    val now: Timestamp
) : ScalarFunction {
    override val signature = FunctionSignature(
        name = "unix_timestamp",
        requiredParameters = emptyList(),
        optionalParameter = listOf(TimeStampType),
        returnType = listOf(IntType, DecimalType)
    )

    private val millisPerSecond = BigDecimal(1000)
    private fun epoch(timestamp: Timestamp): BigDecimal = timestamp.decimalMillis.divide(millisPerSecond)

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        return valueFactory.newInt(epoch(now).toLong())
    }

    override fun callWithOptional(required: List<ExprValue>, optional: ExprValue): ExprValue {
        val timestamp = optional.timestampValue()
        val epochTime = epoch(timestamp)
        return if (timestamp.decimalSecond.scale() == 0) {
            valueFactory.newInt(epochTime.toLong())
        } else {
            valueFactory.newDecimal(epochTime)
        }
    }
}
