package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*


/**
 * IonSQL++ function to convert a formatted string into an Ion Timestamp.
 */
class ToTimestampExprFunction(ion: IonSystem) : NullPropagatingExprFunction("to_timestamp", 1..2, ion) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
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
            args[0].ionValue !is IonString ->
                errNoContext("First argument of to_timestamp is not a string.", internal = false)
            args.size == 2 && args[1].ionValue !is IonString ->
                errNoContext("Second argument of to_timestamp is not a string.", internal = false)
        }
    }
}