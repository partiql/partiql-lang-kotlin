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
        val datetime = args[1]
        val datetimeValue = datetime.time
        val intervalValue = interval.toLong()
        return Datum.timeWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT64_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.typeTimeWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeBigInt()),
            FnParameter("datetime", PType.typeTimeWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.time
        val intervalValue = interval.long
        return Datum.timeWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.typeTimeWithoutTZ(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.typeIntArbitrary()),
            FnParameter("datetime", PType.typeTimeWithoutTZ(6)),
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
        return Datum.timeWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
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
        val datetime = args[1]
        val datetimeValue = datetime.timestamp
        val intervalValue = interval.toLong()
        return Datum.timestampWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.typeTimestampWithoutTZ(6),
        parameters = listOf(
            FnParameter("interval", PType.typeBigInt()),
            FnParameter("datetime", PType.typeTimestampWithoutTZ(6)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0]
        val datetime = args[1]
        val datetimeValue = datetime.timestamp
        val intervalValue = interval.long
        return Datum.timestampWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}

internal object Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = PType.typeTimestampWithoutTZ(6),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("interval", PType.typeIntArbitrary()),
            FnParameter("datetime", PType.typeTimestampWithoutTZ(6)),
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
        return Datum.timestampWithoutTZ(datetimeValue.plusMinutes(intervalValue))
    }
}
