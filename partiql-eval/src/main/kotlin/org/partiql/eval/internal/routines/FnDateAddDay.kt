// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DateValue
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.IntValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DATE
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TIMESTAMP
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.dateValue
import org.partiql.value.timestampValue


internal object Fn_DATE_ADD_DAY__INT_DATE__DATE : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return dateValue(datetimeValue.plusDays(intervalValue))
    }
}


internal object Fn_DATE_ADD_DAY__BIGINT_DATE__DATE : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", BIGINT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return dateValue(datetimeValue.plusDays(intervalValue))
    }
}


internal object Fn_DATE_ADD_DAY__NUMERIC_DATE__DATE : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) {
            throw TypeCheckException()
        }
        return dateValue(datetimeValue.plusDays(intervalValue))
    }
}


internal object Fn_DATE_ADD_DAY__INT_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return timestampValue(datetimeValue.plusDays(intervalValue))
    }
}


internal object Fn_DATE_ADD_DAY__BIGINT_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", BIGINT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return timestampValue(datetimeValue.plusDays(intervalValue))
    }
}


internal object Fn_DATE_ADD_DAY__NUMERIC_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_day",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) {
            throw TypeCheckException()
        }
        return timestampValue(datetimeValue.plusDays(intervalValue))
    }
}
