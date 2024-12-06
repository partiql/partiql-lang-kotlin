package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * DATE_ADD is not SQL spec and `datetime + interval` should be used; I'm only adding this for conformance tests.
 */
internal object FnDateAdd : Function {

    private const val NAME = "date_add"

    private val parameters = arrayOf(
        Parameter.text("part"),
        Parameter("quantity", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    )

    override fun getName(): String = NAME

    override fun getParameters(): Array<Parameter> = parameters

    override fun getReturnType(args: Array<PType>): PType {
        if (args.size != 3) {
            throw IllegalArgumentException("Expected 3 arguments, but received ${args.size}.")
        }
        val dt = args[2]
        return when (dt.code()) {
            PType.TIMESTAMP -> PType.timestamp(dt.precision)
            PType.TIMESTAMPZ -> PType.timestampz(dt.precision)
            else -> throw IllegalArgumentException("Expected timestamp type, but received $dt.")
        }
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        if (args.size != parameters.size) {
            return null // should be unreachable
        }
        return instance
    }

    private val instance = Function.instance(NAME, parameters, PType.timestamp(6)) { args ->
        val part = args[0].string
        val quantity = args[1].int.toLong()
        val dt = args[2].localDateTime
        val result = when (part) {
            "year" -> dt.plusYears(quantity)
            "month" -> dt.plusMonths(quantity)
            "day" -> dt.plusDays(quantity)
            "hour" -> dt.plusHours(quantity)
            "minute" -> dt.plusMinutes(quantity)
            "second" -> dt.plusSeconds(quantity)
            else -> throw IllegalArgumentException("Expected part to be one of: year, month, day, hour, minute, second, millisecond, but received $part.")
        }
        Datum.timestamp(result, 6)
    }
}
