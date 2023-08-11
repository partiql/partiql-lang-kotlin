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

import org.partiql.errors.ErrorCode
import org.partiql.lang.types.FunctionSignature

sealed class Arguments
data class RequiredArgs(val required: List<ExprValue>) : Arguments()
data class RequiredWithOptional(val required: List<ExprValue>, val opt: ExprValue) : Arguments()
data class RequiredWithVariadic(val required: List<ExprValue>, val variadic: List<ExprValue>) : Arguments()

/**
 * Represents a function that can be invoked from within an [EvaluatingCompiler]
 * compiled [Expression].
 *
 * Note that [ExprFunction] implementations do not need to deal with propagation of
 * unknown values `MISSING` or `NULL` as this is handled by [EvaluatingCompiler].
 */
interface ExprFunction {
    /**
     * Static signature of this function.
     *
     */
    val signature: FunctionSignature

    /**
     * Invokes the function with its required parameters only.
     *
     * It is assumed that the [ExprFunction]s will always be called after validating
     * the arguments for the `arity` and the types of arguments.
     * [EvaluatingCompiler] validates the arguments before calling [ExprFunction]s.
     * Hence the implementations are not required to deal with it.
     *
     * @param session The current [EvaluationSession].
     * @param required The required arguments.
     */
    fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including only required parameters
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    /**
     * Invokes the function with its required parameters and an optional parameter.
     *
     * It is assumed that the [ExprFunction]s will always be called after validating
     * the arguments for the `arity` and the types of arguments.
     * [EvaluatingCompiler] validates the arguments before calling [ExprFunction]s.
     * Hence the implementations are not required to deal with it.
     *
     * @param session The current [EvaluationSession].
     * @param required The required arguments.
     * @param opt The optional arguments.
     */
    fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including required parameters and optional
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    /**
     * Invokes the function with its required parameters and any variadic parameters.
     *
     * It is assumed that the [ExprFunction]s will always be called after validating
     * the arguments for the `arity` and the types of arguments.
     * [EvaluatingCompiler] validates the arguments before calling [ExprFunction]s.
     * Hence the implementations are not required to deal with it.
     *
     * @param session The current [EvaluationSession].
     * @param required The required arguments.
     * @param variadic The variadic arguments.
     */
    fun callWithVariadic(session: EvaluationSession, required: List<ExprValue>, variadic: List<ExprValue>): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including required parameters and variadic
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }
}

/**
 * Invokes the function.
 *
 * It is assumed that the [ExprFunction]s will always be called after validating
 * the arguments for the `arity` and the types of arguments.
 * [EvaluatingCompiler] validates the arguments before calling [ExprFunction]s.
 * Hence the implementations are not required to deal with it.
 *
 * @param session The current [EvaluationSession].
 * @param args The argument list.
 */
fun ExprFunction.call(session: EvaluationSession, args: Arguments): ExprValue =
    when (args) {
        is RequiredArgs -> callWithRequired(session, args.required)
        is RequiredWithOptional -> callWithOptional(session, args.required, args.opt)
        is RequiredWithVariadic -> callWithVariadic(session, args.required, args.variadic)
    }
