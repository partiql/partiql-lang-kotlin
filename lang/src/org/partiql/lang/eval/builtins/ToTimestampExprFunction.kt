/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*


/**
 * PartiQL function to convert a formatted string into an Ion Timestamp.
 */
class ToTimestampExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("to_timestamp", 1..2, valueFactory) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        validateArguments(args)

        return valueFactory.newTimestamp(when (args.count()) {
            1 -> try {
                Timestamp.valueOf(args[0].ionValue.stringValue())
            } catch(ex: IllegalArgumentException) {
                throw EvaluationException("Timestamp was not a valid ion timestamp",
                                          ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
                                          PropertyValueMap(),
                                          ex,
                                          true)
            }
            else -> TimestampParser.parseTimestamp(args[0].ionValue.stringValue()!!, args[1].ionValue.stringValue()!!)
        })
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