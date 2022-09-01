package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.functions.timestamp.TimestampTemporalAccessor
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.stringValue
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import java.time.temporal.UnsupportedTemporalTypeException

object ToString : ScalarFunction {
    override val signature = FunctionSignature(
        name = "to_string",
        requiredParameters = listOf(listOf(TimeStampType), listOf(StringType)),
        returnType = listOf(StringType)
    )

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        val pattern = required[1].ionValue.stringValue()!!

        val formatter: DateTimeFormatter = try {
            DateTimeFormatter.ofPattern(pattern)
        } catch (ex: IllegalArgumentException) {
            errInvalidFormatPattern(pattern, ex)
        }

        val timestamp = required[0].timestampValue()
        val temporalAccessor = TimestampTemporalAccessor(timestamp)
        try {
            return valueFactory.newString(formatter.format(temporalAccessor))
        } catch (ex: UnsupportedTemporalTypeException) {
            errInvalidFormatPattern(pattern, ex)
        } catch (ex: DateTimeException) {
            errInvalidFormatPattern(pattern, ex)
        }
    }

    private fun errInvalidFormatPattern(pattern: String, cause: Exception): Nothing {
        val pvmap = PropertyValueMap()
        pvmap[Property.TIMESTAMP_FORMAT_PATTERN] = pattern
        throw EvaluationException(
            "Invalid DateTime format pattern",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
            pvmap,
            cause,
            internal = false
        )
    }
}
