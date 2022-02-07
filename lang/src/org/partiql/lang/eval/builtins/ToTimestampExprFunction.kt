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

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.stringValue


/**
 * PartiQL function to convert a formatted string into an Ion Timestamp.
 */
class ToTimestampExprFunction(private val valueFactory: ExprValueFactory) : ExprFunction {
    override val signature = FunctionSignature(
        name = "to_timestamp",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.STRING,
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(env: Environment, required: List<ExprValue>): ExprValue {
        val ts = try {
            Timestamp.valueOf(required[0].ionValue.stringValue())
        } catch(ex: IllegalArgumentException) {
            throw EvaluationException("Timestamp was not a valid ion timestamp",
                ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
                PropertyValueMap(),
                ex,
                true)
        }
        return valueFactory.newTimestamp(ts)
    }

    override fun callWithOptional(env: Environment, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val ts = TimestampParser.parseTimestamp(required[0].ionValue.stringValue()!!, opt.ionValue.stringValue()!!)
        return valueFactory.newTimestamp(ts)
    }
}