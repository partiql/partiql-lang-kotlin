// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DateValue
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.dateValue
import org.partiql.value.timestampValue

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT32_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return dateValue(datetimeValue.plusMonths(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT64_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.value!!
        return dateValue(datetimeValue.plusMonths(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT_DATE__DATE : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = DATE,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<DateValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = try {
            interval.toInt64().value!!
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return dateValue(datetimeValue.plusMonths(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.toInt64().value!!
        return timestampValue(datetimeValue.plusMonths(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.value!!
        return timestampValue(datetimeValue.plusMonths(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_month",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = try {
            interval.toInt64().value!!
        } catch (e: DataException) {
            throw TypeCheckException()
        }
        return timestampValue(datetimeValue.plusMonths(intervalValue))
    }
}
