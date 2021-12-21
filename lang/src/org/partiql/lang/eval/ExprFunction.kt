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

package org.partiql.lang.eval

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.util.isAnyUnknown

/**
 * Represents a function that can be invoked from within an [EvaluatingCompiler]
 * compiled [Expression].
 */
interface ExprFunction {
    /**
     * The name that can be used to reference this function within queries.
     */
    val name: String

    /**
     * Invokes the function.
     *
     * Implementations are required to deal with being called with the wrong number
     * of arguments or the wrong argument types.
     *
     * @param env The calling environment.
     * @param args The argument list.
     */
    fun call(env: Environment, args: List<ExprValue>): ExprValue
}

/**
 * [ExprFunction] template that checks arity and propagates null arguments when any parameter is either null or
 * missing
 *
 * @param name function name
 * @param arity function arity
 * @param ion current Ion system
 */
abstract class NullPropagatingExprFunction(override val name: String,
                                           override val arity: IntRange,
                                           val valueFactory: ExprValueFactory) : ArityCheckingTrait, ExprFunction {

    constructor(name: String, arity: Int, valueFactory: ExprValueFactory) : this(name, (arity..arity), valueFactory)

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args.isAnyUnknown() -> valueFactory.nullValue
            else                -> eval(env, args)
        }
    }

    abstract fun eval(env: Environment, args: List<ExprValue>): ExprValue
}

/**
 * "Trait" that provides [checkArity] function to validate a function arity
 */
internal interface ArityCheckingTrait {
    val name: String
    val arity: IntRange

    private fun arityErrorMessage(argSize: Int) = when {
        arity.first == 1 && arity.last == 1 -> "$name takes a single argument, received: $argSize"
        arity.first == arity.last           -> "$name takes exactly ${arity.first} arguments, received: $argSize"
        else                                -> "$name takes between ${arity.first} and ${arity.last} arguments, received: $argSize"
    }

    fun checkArity(args: List<ExprValue>) {
        if (!arity.contains(args.size)) {
            val errorContext = PropertyValueMap()
            errorContext[Property.EXPECTED_ARITY_MIN] = arity.first
            errorContext[Property.EXPECTED_ARITY_MAX] = arity.last

            throw EvaluationException(arityErrorMessage(args.size),
                                      ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                      errorContext,
                                      internal = false)
        }
    }
}