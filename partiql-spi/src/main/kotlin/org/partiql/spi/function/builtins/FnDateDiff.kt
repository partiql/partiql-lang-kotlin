package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.time.temporal.ChronoUnit

/**
 * DATE_DIFF is not SQL standard, so I am doing the bare minimum for unit tests.
 *
 * This does not do precise typing, you always get timestamp(6).
 */
internal object FnDateDiff : Function {

    private const val NAME = "date_diff"

    private val parameters = arrayOf(
        Parameter("part", PType.string()),
        Parameter("t1", PType.timestamp(6)),
        Parameter("t2", PType.timestamp(6)),
    )

    override fun getName(): String = NAME

    override fun getParameters(): Array<Parameter> = parameters

    override fun getReturnType(args: Array<PType>): PType = PType.integer()

    /**
     * once again switching on the first argument because this is a non-standard function.
     */
    override fun getInstance(args: Array<PType>): Function.Instance? {
        if (args.size != parameters.size) {
            return null // should be unreachable
        }
        return instance
    }

    private val instance = Function.instance(NAME, parameters, PType.integer()) { args ->
        val part = args[0].string
        val t1 = args[1].localDateTime
        val t2 = args[2].localDateTime
        val result = when (part) {
            "year" -> ChronoUnit.YEARS.between(t1, t2)
            "month" -> ChronoUnit.MONTHS.between(t1, t2)
            "day" -> ChronoUnit.DAYS.between(t1, t2)
            "hour" -> ChronoUnit.HOURS.between(t1, t2)
            "minute" -> ChronoUnit.MINUTES.between(t1, t2)
            "second" -> ChronoUnit.SECONDS.between(t1, t2)
            else -> throw IllegalArgumentException("Expected part to be one of: year, month, day, hour, minute, second, millisecond, but received $part.")
        }
        Datum.integer(result.toInt())
    }
}
