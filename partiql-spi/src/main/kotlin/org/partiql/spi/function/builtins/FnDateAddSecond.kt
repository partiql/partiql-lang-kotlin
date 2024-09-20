// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_DATE_ADD_SECOND__INT32_TIME__TIME : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.time(6),
        parameters = listOf(
            Parameter("interval", PType.integer()),
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1].time
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.time(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT64_TIME__TIME : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.time(6),
        parameters = listOf(
            Parameter("interval", PType.bigint()),
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].time
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.time(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT_TIME__TIME : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.time(6),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
            Parameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].bigInteger
        val datetime = args[1].time
        val datetimeValue = datetime
        val intervalValue = try {
            interval.toLong()
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return Datum.time(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.timestamp(6),
        parameters = listOf(
            Parameter("interval", PType.integer()),
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.timestamp(6),
        parameters = listOf(
            Parameter("interval", PType.bigint()),
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP : Function {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.timestamp(6),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
            Parameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].bigInteger
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = try {
            interval.toLong()
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
    }
}
