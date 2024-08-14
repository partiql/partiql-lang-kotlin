// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_DATE_ADD_MINUTE__INT32_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.time(6),
        parameters = listOf(
            FnParameter("interval", PType.integer()),
            FnParameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1]
        val datetimeValue = datetime.time
        val intervalValue = interval.toLong()
        return Datum.time(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT64_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.time(6),
        parameters = listOf(
            FnParameter("interval", PType.bigint()),
            FnParameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.time
        val intervalValue = interval.long
        return Datum.time(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.time(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.numeric()),
            FnParameter("datetime", PType.time(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.time
        val intervalValue = try {
            interval.bigInteger.toLong()
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return Datum.time(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.timestamp(6),
        parameters = listOf(
            FnParameter("interval", PType.integer()),
            FnParameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1]
        val datetimeValue = datetime.timestamp
        val intervalValue = interval.toLong()
        return Datum.timestamp(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.timestamp(6),
        parameters = listOf(
            FnParameter("interval", PType.bigint()),
            FnParameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.timestamp
        val intervalValue = interval.long
        return Datum.timestamp(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.timestamp(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.numeric()),
            FnParameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.timestamp
        val intervalValue = try {
            interval.bigInteger.toLong()
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return Datum.timestamp(datetimeValue.plusMinutes(intervalValue))
    }
}
