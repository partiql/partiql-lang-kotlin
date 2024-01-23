// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.timeValue
import org.partiql.value.timestampValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT32_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusHours(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT64_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusHours(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT_TIME__TIME : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        // TODO: We need to consider overflow here
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusHours(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
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
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusHours(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
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
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusHours(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "date_add_hour",
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
        // TODO: We need to consider overflow here
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusHours(intervalValue))
    }
}
