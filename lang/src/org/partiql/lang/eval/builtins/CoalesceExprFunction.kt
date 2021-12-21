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

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType

/**
 * Coalesce built in function. Takes in one ore more expression as arguments and returns the first non unknown value
 *
 * ```
 * COALESCE(EXPRESSION, [EXPRESSION...])
 * ```
 */
internal class CoalesceExprFunction(private val valueFactory: ExprValueFactory) : ExprFunction {
    override val name = "coalesce"
    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return args.filterNot { it.type == ExprValueType.NULL || it.type == ExprValueType.MISSING }
                   .firstOrNull() ?: return valueFactory.nullValue
    }

    private fun checkArity(args: List<ExprValue>) {
        if (args.isEmpty()) {
            val errorContext = PropertyValueMap()
            errorContext[Property.EXPECTED_ARITY_MIN] = 1
            errorContext[Property.EXPECTED_ARITY_MAX] = Int.MAX_VALUE

            throw EvaluationException("coalesce requires at least one argument",
                                      ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                      errorContext,
                                      internal = false)
        }
    }
}