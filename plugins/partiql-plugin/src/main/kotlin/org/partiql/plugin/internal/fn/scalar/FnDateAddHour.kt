// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT32_TIME__TIME : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FunctionParameter("interval", INT32),
            FunctionParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimeValue>()
        return if (datetime.value == null || interval.value == null) {
            timeValue(null)
        } else {
            val datetimeValue = datetime.value!!
            val intervalValue = interval.toInt64().value!!
            timeValue(datetimeValue.plusHours(intervalValue))
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT64_TIME__TIME : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FunctionParameter("interval", INT64),
            FunctionParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimeValue>()
        return if (datetime.value == null || interval.value == null) {
            timeValue(null)
        } else {
            val datetimeValue = datetime.value!!
            val intervalValue = interval.value!!
            timeValue(datetimeValue.plusHours(intervalValue))
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT_TIME__TIME : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIME,
        parameters = listOf(
            FunctionParameter("interval", INT),
            FunctionParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimeValue>()
        return if (datetime.value == null || interval.value == null) {
            timeValue(null)
        } else {
            val datetimeValue = datetime.value!!
            val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) { throw TypeCheckException() }
            timeValue(datetimeValue.plusHours(intervalValue))
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(
            FunctionParameter("interval", INT32),
            FunctionParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimestampValue>()
        return if (datetime.value == null || interval.value == null) {
            timestampValue(null)
        } else {
            val datetimeValue = datetime.value!!
            val intervalValue = interval.toInt64().value!!
            timestampValue(datetimeValue.plusHours(intervalValue))
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(
            FunctionParameter("interval", INT64),
            FunctionParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimestampValue>()
        return if (datetime.value == null || interval.value == null) {
            timestampValue(null)
        } else {
            val datetimeValue = datetime.value!!
            val intervalValue = interval.value!!
            timestampValue(datetimeValue.plusHours(intervalValue))
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "date_add_hour",
        returns = TIMESTAMP,
        parameters = listOf(
            FunctionParameter("interval", INT),
            FunctionParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimestampValue>()
        return if (datetime.value == null || interval.value == null) {
            timestampValue(null)
        } else {
            val datetimeValue = datetime.value!!
            // TODO: We need to consider overflow here
            val intervalValue = try { interval.toInt64().value!! } catch (e: DataException) { throw TypeCheckException() }
            timestampValue(datetimeValue.plusHours(intervalValue))
        }
    }
}
