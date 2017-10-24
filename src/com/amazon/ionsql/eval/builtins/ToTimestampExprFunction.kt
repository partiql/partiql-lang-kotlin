package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*


/**
 * IonSQL++ function to convert a formatted string into an Ion Timestamp.
 */
class ToTimestampExprFunction(private val ion: IonSystem) : ExprFunction {
    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        when {
            args.isAnyMissing() -> return missingExprValue(ion)
            args.isAnyNull()    -> return nullExprValue(ion)
        }

        validateArguments(args)

        return ion.newTimestamp(when (args.count()) {
            1 -> try {
                Timestamp.valueOf(args[0].ionValue.stringValue())
            } catch(ex: IllegalArgumentException) {
                throw EvaluationException("Timestamp was not a valid ion timestamp",
                                          ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
                                          PropertyValueMap(),
                                          ex)
            }
            else -> TimestampParser.parseTimestamp(args[0].ionValue.stringValue()!!, args[1].ionValue.stringValue()!!)
        }).exprValue()
    }

    private fun validateArguments(args: List<ExprValue>) {
        when {
            args.count() >= 1 && args[0].ionValue !is IonString ->
                errNoContext("First argument of to_timestamp is not a string.", internal = false)
            args.count() == 2 && args[1].ionValue !is IonString ->
                errNoContext("Second argument of to_timestamp is not a string.", internal = false)
            args.count() > 2 || args.count() == 0 ->
                throw EvaluationException(
                    "Expected 1 or 2 arguments for to_string instead of ${args.size}.",
                    ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                    PropertyValueMap(),
                    internal = false)
        }
    }
}