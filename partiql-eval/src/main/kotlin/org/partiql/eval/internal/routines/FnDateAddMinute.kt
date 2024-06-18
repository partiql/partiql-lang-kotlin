// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.IntValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TIME
import org.partiql.value.PType.Kind.TIMESTAMP
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.timeValue
import org.partiql.value.timestampValue


internal object Fn_DATE_ADD_MINUTE__INT_TIME__TIME : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return timeValue(datetimeValue.plusMinutes(intervalValue))
    }
}


internal object Fn_DATE_ADD_MINUTE__BIGINT_TIME__TIME : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", BIGINT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.value!!
        return timeValue(datetimeValue.plusMinutes(intervalValue))
    }
}


internal object Fn_DATE_ADD_MINUTE__NUMERIC_TIME__TIME : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) { throw TypeCheckException() }
        return timeValue(datetimeValue.plusMinutes(intervalValue))
    }
}


internal object Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
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
        return timestampValue(datetimeValue.plusMinutes(intervalValue))
    }
}


internal object Fn_DATE_ADD_MINUTE__BIGINT_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
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
        val intervalValue = interval.value!!
        return timestampValue(datetimeValue.plusMinutes(intervalValue))
    }
}


internal object Fn_DATE_ADD_MINUTE__NUMERIC_TIMESTAMP__TIMESTAMP : Routine {

    override val signature = FnSignature(
        name = "date_add_minute",
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
        val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) { throw TypeCheckException() }
        return timestampValue(datetimeValue.plusMinutes(intervalValue))
    }
}
