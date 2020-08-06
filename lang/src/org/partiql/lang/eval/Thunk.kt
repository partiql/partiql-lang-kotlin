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

import org.partiql.lang.ast.*
import org.partiql.lang.errors.*


/**
 * A thunk with no parameters other than the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment`.
 */
typealias ThunkEnv = (Environment) -> ExprValue

/**
 * A thunk taking a single [T] argument and the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment` and an [ExprValue] as
 * its arguments.
 */
typealias ThunkEnvValue<T> = (Environment, T) -> ExprValue

/**
 * A type alias for an exception handler.
 */
typealias ThunkExceptionHandler = (Throwable, SourceLocationMeta?) -> Nothing

/**
 * Options for thunk construction. Takes an argument of type [ThunkExceptionHandler]
 *
 * The default exception handler wraps any [Throwable] exception and throws [EvaluationException]
 */
data class ThunkOptions private constructor(
        val handleException: ThunkExceptionHandler = defaultExceptionHandler
) {
    companion object {

        /**
         * Creates a java style builder that will choose the default values for any unspecified options.
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Kotlin style builder that will choose the default values for any unspecified options.
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Creates a [ThunkOptions] instance with the standard values.
         */
        @JvmStatic
        fun standard() = Builder().build()
    }

    /**
     * Builds a [ThunkOptions] instance.
     */
    class Builder {
        private var options = ThunkOptions()
        fun handleException(value: ThunkExceptionHandler) = set { copy(handleException = value)}
        private inline fun set(block: ThunkOptions.() -> ThunkOptions) : Builder {
            options = block(options)
            return this
        }

        fun build() = options
    }
}

internal val defaultExceptionHandler: ThunkExceptionHandler = { e, sourceLocation ->
    val message = e.message ?: "<NO MESSAGE>"
    throw EvaluationException(
        "Internal error, $message",
        errorContext = errorContextFrom(sourceLocation),
        cause = e,
        internal = true)
}

/**
 * Provides methods for constructing new thunks according to the specified [ThunkOptions].
 */
class ThunkFactory(val thunkOptions: ThunkOptions) {

    /**
     * Creates a [ThunkEnv], which handles exceptions by wrapping them into an [EvaluationException] which uses
     * [handleException] to handle exceptions appropriately.
     *
     * Literal lambdas passed to this function as [t] are inlined into the body of the function being returned, which
     * reduces the need to create additional call contexts.  The lambdas passed as [t] may not contain non-local returns
     * (`crossinline`).
     */
    internal inline fun thunkEnv(metas: MetaContainer, crossinline t: ThunkEnv): ThunkEnv {
        val sourceLocationMeta = metas.find(SourceLocationMeta.TAG) as? SourceLocationMeta

        return { env ->
            handleException(sourceLocationMeta) {
                t(env)
            }
        }
    }

    /**
     * Similar to [thunkEnv], but creates a [ThunkEnvValue] instead.
     */
    internal inline fun <T> thunkEnvValue(metas: MetaContainer, crossinline t: ThunkEnvValue<T>): ThunkEnvValue<T> {
        val sourceLocationMeta = metas.find(SourceLocationMeta.TAG) as? SourceLocationMeta

        return { env, arg1 ->
            handleException(sourceLocationMeta) {
                t(env, arg1)
            }
        }
    }

    /**
     * Similar to [thunkEnv] but evaluates all [argThunks] and performs a fold using [op] as the operation.
     *
     * Also handles null propagation appropriately for [NAryOp] arithmetic operations.
     *
     * Note that [argThunks] must contain at least one element.
     */
    internal inline fun thunkFold(
        nullValue: ExprValue,
        metas: MetaContainer,
        argThunks: List<ThunkEnv>,
        crossinline op: (ExprValue, ExprValue) -> ExprValue
    ): ThunkEnv {
        require(argThunks.isNotEmpty()) { "argThunks must not be empty" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)

        return thunkEnv(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                firstValue.isUnknown() -> nullValue
                else -> {
                    otherThunks.fold(firstValue) { acc, curr ->
                        val currValue = curr(env)
                        if (currValue.type.isUnknown) {
                            return@thunkBlock nullValue
                        }
                        op(acc, currValue)
                    }
                }
            }
        }
    }

    /**
     * Similar to [thunkFold] but intended for comparison operators, i.e. `=`, `>`, `>=`, `<`, `<=`.
     *
     * The first argument of [op] is always the value of `argThunks[n]` and
     * the second is always `argThunks[n + 1]` where `n` is 0 to `argThunks.size - 2`.
     *
     * If [op] returns false, the thunk short circuits and the result of the thunk becomes `false`.  If any [argThunks]
     * returns an unknown value then the thunk short circuits and  `NULL` is returned. If [op] is true for all invocations
     * then the result of the thunk becomes `true`.
     *
     * Note that [argThunks] must contain at least two elements.
     *
     * The name of this function was inspired by Racket's `andmap` procedure.
     */
    internal inline fun thunkAndMap(
        valueFactory: ExprValueFactory,
        metas: MetaContainer,
        argThunks: List<ThunkEnv>,
        crossinline op: (ExprValue, ExprValue) -> Boolean
    ): ThunkEnv {
        require(argThunks.size >= 2) { "argThunks must have at least two elements" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)

        return thunkEnv(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                //If the first value is unknown, short circuit returning null.
                firstValue.isUnknown() -> valueFactory.nullValue
                else -> {
                    otherThunks.fold(firstValue) { lastValue, currentThunk ->

                        val currentValue = currentThunk(env)
                        if (currentValue.isUnknown()) {
                            return@thunkBlock valueFactory.nullValue
                        }

                        val result = op(lastValue, currentValue)
                        if (!result) {
                            return@thunkBlock valueFactory.newBoolean(false)
                        }

                        currentValue
                    }

                    valueFactory.newBoolean(true)
                }

            }
        }
    }


    /**
     * Handles exceptions appropriately for a run-time [ThunkEnv].
     *
     * - The [SourceLocationMeta] will be extracted from [MetaContainer] and included in any [EvaluationException] that
     * is thrown, if present.
     * - The location information is added to the [EvaluationException]'s `errorContext`, if it is not already present.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    internal inline fun handleException(
        sourceLocation: SourceLocationMeta?,
        crossinline block: () -> ExprValue
    ): ExprValue =
        try {
            block()
        } catch (e: EvaluationException) {
            when {
                e.errorContext == null ->
                    throw EvaluationException(
                        message = e.message,
                        errorCode = e.errorCode,
                        errorContext = errorContextFrom(sourceLocation),
                        cause = e,
                        internal = e.internal)
                else -> {
                    // Only add source location data to the error context if it doesn't already exist
                    // in [errorContext].
                    if (!e.errorContext.hasProperty(Property.LINE_NUMBER)) {
                        sourceLocation?.let { fillErrorContext(e.errorContext, sourceLocation) }
                    }
                    throw e
                }
            }
        } catch (e: Exception) {
            thunkOptions.handleException(e, sourceLocation)
        }
}
