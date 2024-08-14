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

internal object Fn_DATE_ADD_MONTH__INT32_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = PType.date(),
        parameters = listOf(
            FnParameter("interval", PType.integer()),
            FnParameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1].date
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.date(datetimeValue.plusMonths(intervalValue))
    }
}

internal object Fn_DATE_ADD_MONTH__INT64_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = PType.date(),
        parameters = listOf(
            FnParameter("interval", PType.bigint()),
            FnParameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].date
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.date(datetimeValue.plusMonths(intervalValue))
    }
}

internal object Fn_DATE_ADD_MONTH__INT_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = PType.date(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.numeric()),
            FnParameter("datetime", PType.date()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].bigInteger
        val datetime = args[1].date
        val datetimeValue = datetime
        val intervalValue = try {
            interval.toLong()
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return Datum.date(datetimeValue.plusMonths(intervalValue))
    }
}

internal object Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
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
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.timestamp(datetimeValue.plusMonths(intervalValue))
    }
}

internal object Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = PType.timestamp(6),
        parameters = listOf(
            FnParameter("interval", PType.bigint()),
            FnParameter("datetime", PType.timestamp(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.timestamp(datetimeValue.plusMonths(intervalValue))
    }
}

internal object Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = PType.timestamp(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.numeric()),
            FnParameter("datetime", PType.timestamp(6)),
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
        return Datum.timestamp(datetimeValue.plusMonths(intervalValue))
    }
}
