package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import java.lang.IllegalArgumentException
import java.time.*
import java.time.format.*
import java.time.temporal.*

class ToStringExprFunction(ion: IonSystem) : NullPropagatingExprFunction("to_string", 2, ion) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        validateArguments(args)

        val pattern = args[1].ionValue.stringValue()!!

        val formatter: DateTimeFormatter = try {
            DateTimeFormatter.ofPattern(pattern)
        }
        catch (ex: IllegalArgumentException) {
            errInvalidFormatPattern(pattern, ex)
        }

        val timestamp = args[0].timestampValue()
        val temporalAccessor = TimestampTemporalAccessor(timestamp)
        try {
            return formatter.format(temporalAccessor).exprValue(ion)
        }
        catch (ex: UnsupportedTemporalTypeException) {
            errInvalidFormatPattern(pattern, ex)
        } catch (ex: DateTimeException) {
            errInvalidFormatPattern(pattern, ex)
        }
    }

    private fun validateArguments(args: List<ExprValue>) {
        when {
            args[0].ionValue !is IonTimestamp -> errNoContext("First argument of to_string is not a timestamp.", internal = false)
            args[1].ionValue !is IonText      -> errNoContext("Second argument of to_string is not a string.", internal = false)
        }
    }

    private fun errInvalidFormatPattern(pattern: String, cause: Exception): Nothing {
        val pvmap = PropertyValueMap()
        pvmap[Property.TIMESTAMP_FORMAT_PATTERN] = pattern
        throw EvaluationException("Invalid DateTime format pattern",
                                 ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
                                 pvmap,
                                 cause,
                                 internal = false)
    }
}