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

@Deprecated("As function overloading is now supported, this class is deprecated.", level = DeprecationLevel.ERROR)
sealed class Arguments

@Suppress("DEPRECATION_ERROR")
@Deprecated("As function overloading is now supported, this class is deprecated.", level = DeprecationLevel.ERROR)
data class RequiredArgs(val required: List<ExprValue>) : Arguments()

@Suppress("DEPRECATION_ERROR")
@Deprecated("As function overloading is now supported, this class is deprecated.", level = DeprecationLevel.ERROR)
data class RequiredWithOptional(val required: List<ExprValue>, val opt: ExprValue) : Arguments()

@Suppress("DEPRECATION_ERROR")
@Deprecated("As function overloading is now supported, this class is deprecated.", level = DeprecationLevel.ERROR)
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
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    @Deprecated("Please define overloaded functions by providing each alternative in required parameter as its own ExprFunction; each function is invoked by callWithRequired() rather than callWithOptional().", level = DeprecationLevel.ERROR)
    fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including required parameters and optional
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    @Deprecated("Please define overloaded functions by providing the LIST ExprValue in required parameter to represent variadic parameters as its own ExprFunction; each function is invoked by callWithRequired() rather than callWithVariadic().", level = DeprecationLevel.ERROR)
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
fun ExprFunction.call(session: EvaluationSession, args: List<ExprValue>): ExprValue = callWithRequired(session, args)

@Suppress("DEPRECATION_ERROR")
@Deprecated("As function overloading is now supported, this class is deprecated. Please use use call(session: EvaluationSession, args: List<ExprValue>) instead", level = DeprecationLevel.ERROR)
fun ExprFunction.call(session: EvaluationSession, args: Arguments): ExprValue =
    when (args) {
        is RequiredArgs -> callWithRequired(session, args.required)
        is RequiredWithOptional -> callWithOptional(session, args.required, args.opt)
        is RequiredWithVariadic -> callWithVariadic(session, args.required, args.variadic)
    }
