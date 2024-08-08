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

internal object Fn_DATE_ADD_SECOND__INT32_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimeWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeInt()),
            FnParameter("datetime", PType.typeTimeWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1].time
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.timeWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT64_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimeWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeBigInt()),
            FnParameter("datetime", PType.typeTimeWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].time
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.timeWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimeWithoutTZ(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.typeIntArbitrary()),
            FnParameter("datetime", PType.typeTimeWithoutTZ(6)),
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
        return Datum.timeWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimestampWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeInt()),
            FnParameter("datetime", PType.typeTimestampWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].int
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval.toLong()
        return Datum.timestampWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimestampWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeBigInt()),
            FnParameter("datetime", PType.typeTimestampWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].long
        val datetime = args[1].timestamp
        val datetimeValue = datetime
        val intervalValue = interval
        return Datum.timestampWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}

internal object Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = PType.typeTimestampWithoutTZ(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.typeIntArbitrary()),
            FnParameter("datetime", PType.typeTimestampWithoutTZ(6)),
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
        return Datum.timestampWithoutTZ(datetimeValue.plusSeconds(intervalValue))
    }
}
