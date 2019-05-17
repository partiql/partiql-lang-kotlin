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
import java.lang.IllegalArgumentException
import java.time.*
import java.time.format.*
import java.time.temporal.*

class ToStringExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("to_string", 2, valueFactory) {
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
            return valueFactory.newString(formatter.format(temporalAccessor))
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