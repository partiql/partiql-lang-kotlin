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

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.domains.staticType
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.BuilderDsl

/**
 * A thunk with no parameters other than the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment`.
 * @param TEnv The type of the environment.  Generic so that the legacy AST compiler and the new compiler may use
 * different types here.
 */
internal typealias Thunk<TEnv> = (TEnv) -> ExprValue

/**
 * A thunk taking a single [T] argument and the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment` and an [ExprValue] as
 * its arguments.
 *
 * @param TEnv The type of the environment.  Generic so that the legacy AST compiler and the new compiler may use
 * different types here.
 * @param TArg The type of the additional argument.
 */
internal typealias ThunkValue<TEnv, TArg> = (TEnv, TArg) -> ExprValue

/**
 * A type alias for an exception handler which always throws(primarily used for [TypingMode.LEGACY]).
 */
typealias ThunkExceptionHandlerForLegacyMode = (Throwable, SourceLocationMeta?) -> Nothing

/**
 * A type alias for an exception handler which does not always throw(primarily used for [TypingMode.PERMISSIVE]).
 */
typealias ThunkExceptionHandlerForPermissiveMode = (Throwable, SourceLocationMeta?) -> Unit

/**
 * Options for thunk construction.
 *
 *  - [handleExceptionForLegacyMode] will be called when in [TypingMode.LEGACY] mode
 *  - [handleExceptionForPermissiveMode] will be called when in [TypingMode.PERMISSIVE] mode
 * The default exception handler wraps any [Throwable] exception and throws [EvaluationException]
 */
data class ThunkOptions private constructor(
    val handleExceptionForLegacyMode: ThunkExceptionHandlerForLegacyMode = DEFAULT_EXCEPTION_HANDLER_FOR_LEGACY_MODE,
    val handleExceptionForPermissiveMode: ThunkExceptionHandlerForPermissiveMode = DEFAULT_EXCEPTION_HANDLER_FOR_PERMISSIVE_MODE,
    val thunkReturnTypeAssertions: ThunkReturnTypeAssertions = ThunkReturnTypeAssertions.DISABLED,
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
    @BuilderDsl
    class Builder {
        private var options = ThunkOptions()
        fun handleExceptionForLegacyMode(value: ThunkExceptionHandlerForLegacyMode) = set { copy(handleExceptionForLegacyMode = value) }
        fun handleExceptionForPermissiveMode(value: ThunkExceptionHandlerForPermissiveMode) = set { copy(handleExceptionForPermissiveMode = value) }
        fun evaluationTimeTypeChecks(value: ThunkReturnTypeAssertions) = set { copy(thunkReturnTypeAssertions = value) }
        private inline fun set(block: ThunkOptions.() -> ThunkOptions): Builder {
            options = block(options)
            return this
        }

        fun build() = options
    }
}

val DEFAULT_EXCEPTION_HANDLER_FOR_LEGACY_MODE: ThunkExceptionHandlerForLegacyMode = { e, sourceLocation ->
    val message = e.message ?: "<NO MESSAGE>"
    throw EvaluationException(
        "Internal error, $message",
        errorCode = (e as? EvaluationException)?.errorCode ?: ErrorCode.EVALUATOR_GENERIC_EXCEPTION,
        errorContext = errorContextFrom(sourceLocation),
        cause = e,
        internal = true
    )
}

val DEFAULT_EXCEPTION_HANDLER_FOR_PERMISSIVE_MODE: ThunkExceptionHandlerForPermissiveMode = { _, _ -> }

/**
 * An extension method for creating [ThunkFactory] based on the type of [TypingMode]
 *  - when [TypingMode] is [TypingMode.LEGACY], creates [LegacyThunkFactory]
 *  - when [TypingMode] is [TypingMode.PERMISSIVE], creates [PermissiveThunkFactory]
 */
internal fun <TEnv> TypingMode.createThunkFactory(
    thunkOptions: ThunkOptions,
    valueFactory: ExprValueFactory
): ThunkFactory<TEnv> = when (this) {
    TypingMode.LEGACY -> LegacyThunkFactory(thunkOptions, valueFactory)
    TypingMode.PERMISSIVE -> PermissiveThunkFactory(thunkOptions, valueFactory)
}
/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions].
 */
internal abstract class ThunkFactory<TEnv>(
    val thunkOptions: ThunkOptions,
    val valueFactory: ExprValueFactory
) {
    private fun checkEvaluationTimeType(thunkResult: ExprValue, metas: MetaContainer): ExprValue {
        // When this check is enabled we throw an exception the [MetaContainer] does not have a
        // [StaticTypeMeta].  This indicates a bug or unimplemented support for an AST node in
        // [StaticTypeInferenceVisitorTransform].
        val staticType = metas.staticType?.type ?: error("Metas collection does not have a StaticTypeMeta")
        if (!staticType.isInstance(thunkResult)) {
            throw EvaluationException(
                "Runtime type does not match the expected StaticType",
                ErrorCode.EVALUATOR_VALUE_NOT_INSTANCE_OF_EXPECTED_TYPE,
                errorContext = errorContextFrom(metas).apply {
                    this[Property.EXPECTED_STATIC_TYPE] = staticType.toString()
                },
                internal = true
            )
        }
        return thunkResult
    }

    /**
     * If [ThunkReturnTypeAssertions.ENABLED] is set, wraps the receiver thunk in another thunk
     * that verifies that the value returned from the receiver thunk matches the type found in the [StaticTypeMeta]
     * contained within [metas].
     *
     * If [metas] contains does not contain [StaticTypeMeta], an [IllegalStateException] is thrown. This is to prevent
     * confusion in the case [StaticTypeInferenceVisitorTransform] has a bug which prevents it from assigning a
     * [StaticTypeMeta] or in case it is not run at all.
     */
    protected fun Thunk<TEnv>.typeCheck(metas: MetaContainer): Thunk<TEnv> =
        when (thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> this
            ThunkReturnTypeAssertions.ENABLED -> {
                val wrapper = { env: TEnv ->
                    val thunkResult: ExprValue = this(env)
                    checkEvaluationTimeType(thunkResult, metas)
                }
                wrapper
            }
        }

    /** Same as [typeCheck] but works on a [ThunkEnvValue<ExprValue>] instead of a [Thunk<TEnv>]. */
    protected fun ThunkValue<TEnv, ExprValue>.typeCheckEnvValue(metas: MetaContainer): ThunkValue<TEnv, ExprValue> =
        when (thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> this
            ThunkReturnTypeAssertions.ENABLED -> {
                val wrapper = { env: TEnv, value: ExprValue ->
                    val thunkResult: ExprValue = this(env, value)
                    checkEvaluationTimeType(thunkResult, metas)
                }
                wrapper
            }
        }

    /** Same as [typeCheck] but works on a [ThunkEnvValue<List<ExprValue>>] instead of a [Thunk<TEnv>]. */
    protected fun ThunkValue<TEnv, List<ExprValue>>.typeCheckEnvValueList(metas: MetaContainer): ThunkValue<TEnv, List<ExprValue>> =
        when (thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> this
            ThunkReturnTypeAssertions.ENABLED -> {
                val wrapper = { env: TEnv, value: List<ExprValue> ->
                    val thunkResult: ExprValue = this(env, value)
                    checkEvaluationTimeType(thunkResult, metas)
                }
                wrapper
            }
        }

    /**
     * Creates a [Thunk<TEnv>] which handles exceptions by wrapping them into an [EvaluationException] which uses
     * [handleException] to handle exceptions appropriately.
     *
     * Literal lambdas passed to this function as [t] are inlined into the body of the function being returned, which
     * reduces the need to create additional call contexts.  The lambdas passed as [t] may not contain non-local returns
     * (`crossinline`).
     */
    internal inline fun thunkEnv(metas: MetaContainer, crossinline t: Thunk<TEnv>): Thunk<TEnv> {
        val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta

        return { env: TEnv ->
            handleException(sourceLocationMeta) {
                t(env)
            }
        }.typeCheck(metas)
    }

    /**
     * Defines the strategy for unknown propagation of 1-3 operands.
     *
     * This is the [TypingMode] specific implementation of unknown-propagation, used by the [thunkEnvOperands]
     * functions.  [getVal1], [getVal2] and [getVal2] are lambdas to allow for differences in short-circuiting.
     *
     * For all [TypingMode]s, if the values returned by [getVal1], [getVal2] and [getVal2] are all known,
     * [compute] is invoked to perform the operation-specific computation.
     */
    protected abstract fun propagateUnknowns(
        getVal1: () -> ExprValue,
        getVal2: (() -> ExprValue)?,
        getVal3: (() -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue

    /**
     * Similar to the other [propagateUnknowns] overload, performs unknown propagation for a variadic sequence of
     * operations.
     */
    protected abstract fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue

    /**
     * Creates a thunk that accepts three [Thunk<TEnv>] operands ([t1], [t2], and [t3]), evaluates them and propagates
     * unknowns according to the current [TypingMode].  When possible, use this function or one of its overloads
     * instead of [thunkEnv] when the operation requires propagation of unknown values.
     *
     * [t1], [t2] and [t3] are each evaluated in with short circuiting depending on the current [TypingMode]:
     *
     * - In [TypingMode.PERMISSIVE] mode, the first `MISSING` returned from one of the thunks causes a short-circuit,
     * and `MISSING` is returned immediately without evaluating the remaining thunks.  If none of the thunks return
     * `MISSING`, if any of them has returned `NULL`, `NULL` is returned.
     * - In [TypingMode.LEGACY] mode, the first `NULL` or `MISSING` returned from one of the thunks causes a
     * short-circuit, and returns `NULL` without evaluating the remaining thunks.
     *
     * In both modes, if none of the thunks returns `MISSING` or `NULL`, [compute] is invoked to perform the final
     * computation on values of the operands which are guaranteed to be known.
     *
     * Overloads of this function exist that accept 1 and 2 arguments.  We do not make [t2] and [t3] nullable with a
     * default value of `null` instead of supplying those overloads primarily because [compute] has a different
     * signature for each, but also because that would prevent [thunkEnvOperands] from being `inline`.
     */
    internal inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: Thunk<TEnv>,
        crossinline t2: Thunk<TEnv>,
        crossinline t3: Thunk<TEnv>,
        crossinline compute: (TEnv, ExprValue, ExprValue, ExprValue) -> ExprValue
    ): Thunk<TEnv> =
        thunkEnv(metas) { env ->
            propagateUnknowns({ t1(env) }, { t2(env) }, { t3(env) }) { v1, v2, v3 ->
                compute(env, v1, v2!!, v3!!)
            }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with three [Thunk<TEnv>] operands. */
    internal inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: Thunk<TEnv>,
        crossinline t2: Thunk<TEnv>,
        crossinline compute: (TEnv, ExprValue, ExprValue) -> ExprValue
    ): Thunk<TEnv> =
        this.thunkEnv(metas) { env ->
            propagateUnknowns({ t1(env) }, { t2(env) }, null) { v1, v2, _ ->
            compute(env, v1, v2!!)
        }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with three [Thunk<TEnv>] operands. */
    internal inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: Thunk<TEnv>,
        crossinline compute: (TEnv, ExprValue) -> ExprValue
    ): Thunk<TEnv> =
        this.thunkEnv(metas) { env ->
            propagateUnknowns({ t1(env) }, null, null) { v1, _, _ ->
            compute(env, v1)
        }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with a variadic list of [Thunk<TEnv>] operands. */
    internal inline fun thunkEnvOperands(
        metas: MetaContainer,
        operandThunks: List<Thunk<TEnv>>,
        crossinline compute: (TEnv, List<ExprValue>) -> ExprValue
    ): Thunk<TEnv> {

        return this.thunkEnv(metas) { env ->
            val operandSeq = sequence { operandThunks.forEach { yield(it(env)) } }
            propagateUnknowns(operandSeq) { values ->
                compute(env, values)
            }
        }.typeCheck(metas)
    }

    /** Similar to [thunkEnv], but creates a [ThunkEnvValue<ExprValue>] instead. */
    internal inline fun thunkEnvValue(
        metas: MetaContainer,
        crossinline t: ThunkValue<TEnv, ExprValue>
    ): ThunkValue<TEnv, ExprValue> {
        val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta

        return { env: TEnv, arg1: ExprValue ->
            handleException(sourceLocationMeta) {
                t(env, arg1)
            }
        }.typeCheckEnvValue(metas)
    }

    /** Similar to [thunkEnv], but creates a [ThunkEnvValue<List<ExprValue>>] instead. */
    internal inline fun thunkEnvValueList(
        metas: MetaContainer,
        crossinline t: ThunkValue<TEnv, List<ExprValue>>
    ): ThunkValue<TEnv, List<ExprValue>> {
        val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta

        return { env: TEnv, arg1: List<ExprValue> ->
            handleException(sourceLocationMeta) {
                t(env, arg1)
            }
        }.typeCheckEnvValueList(metas)
    }

    /**
     * Similar to [thunkEnv] but evaluates all [argThunks] and performs a fold using [op] as the operation.
     *
     * Also handles null propagation appropriately for [NAryOp] arithmetic operations.  Each thunk in [argThunks]
     * is evaluated in turn and:
     *
     * - for [TypingMode.LEGACY], the first unknown operand short-circuits, returning `NULL`.
     * - for [TypingMode.PERMISSIVE], the first missing operand short-circuits, returning `MISSING`.  Then, if one
     * of the operands returned `NULL`, `NULL` is returned.
     *
     * For both modes, if all of the operands are known, performs a fold over them with [op].
     */
    internal abstract fun thunkFold(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): Thunk<TEnv>

    /**
     * Similar to [thunkFold] but intended for comparison operators, i.e. `=`, `>`, `>=`, `<`, `<=`.
     *
     * The first argument of [op] is always the value of `argThunks[n]` and
     * the second is always `argThunks[n + 1]` where `n` is 0 to `argThunks.size - 2`.
     *
     * - If [op] returns false, the thunk short circuits and the result of the thunk becomes `false`.
     * - for [TypingMode.LEGACY], the first unknown operand short-circuits, returning `NULL`.
     * - for [TypingMode.PERMISSIVE], the first missing operand short-circuits, returning `MISSING`.  Then, if one
     * of the operands returned `NULL`, `NULL` is returned.
     *
     * If [op] is true for all invocations then the result of the thunk becomes `true`, otherwise the reuslt is `false`.
     *
     * The name of this function was inspired by Racket's `andmap` procedure.
     */
    internal abstract fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): Thunk<TEnv>

    /**
     * Handles exceptions appropriately for a run-time [Thunk<TEnv>].
     *
     * - The [SourceLocationMeta] will be extracted from [MetaContainer] and included in any [EvaluationException] that
     * is thrown, if present.
     * - The location information is added to the [EvaluationException]'s `errorContext`, if it is not already present.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    abstract fun handleException(
        sourceLocation: SourceLocationMeta?,
        block: () -> ExprValue
    ): ExprValue
}

/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions] for [TypingMode.LEGACY] behaviour.
 */
internal class LegacyThunkFactory<TEnv>(
    thunkOptions: ThunkOptions,
    valueFactory: ExprValueFactory
) : ThunkFactory<TEnv>(thunkOptions, valueFactory) {

    override fun propagateUnknowns(
        getVal1: () -> ExprValue,
        getVal2: (() -> ExprValue)?,
        getVal3: (() -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue {
        val val1 = getVal1()
        return when {
            val1.isUnknown() -> valueFactory.nullValue
            else -> {
                val val2 = getVal2?.let { it() }
                when {
                    val2 == null -> compute(val1, null, null)
                    val2.isUnknown() -> valueFactory.nullValue
                    else -> {
                        val val3 = getVal3?.let { it() }
                        when {
                            val3 == null -> compute(val1, val2, null)
                            val3.isUnknown() -> valueFactory.nullValue
                            else -> compute(val1, val2, val3)
                        }
                    }
                }
            }
        }
    }

    override fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue {
        // Because we need to short-circuit on the first unknown value and [operands] is a sequence,
        // we can't use .map here.  (non-local returns on `.map` are not allowed)
        val argValues = mutableListOf<ExprValue>()
        operands.forEach {
            when {
                it.isUnknown() -> return valueFactory.nullValue
                else -> argValues.add(it)
            }
        }
        return compute(argValues)
    }

    /** See [ThunkFactory.thunkFold]. */
    override fun thunkFold(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): Thunk<TEnv> {
        require(argThunks.isNotEmpty()) { "argThunks must not be empty" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)
        return thunkEnv(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                // Short-circuit at first NULL or MISSING value and return NULL.
                firstValue.isUnknown() -> valueFactory.nullValue
                else -> {
                    otherThunks.fold(firstValue) { acc, curr ->
                        val currValue = curr(env)
                        if (currValue.type.isUnknown) {
                            return@thunkBlock valueFactory.nullValue
                        }
                        op(acc, currValue)
                    }
                }
            }
        }.typeCheck(metas)
    }

    /** See [ThunkFactory.thunkAndMap]. */
    override fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): Thunk<TEnv> {
        require(argThunks.size >= 2) { "argThunks must have at least two elements" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)

        return thunkEnv(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                // If the first value is unknown, short circuit returning null.
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
     * Handles exceptions appropriately for a run-time [Thunk<TEnv>] respecting [TypingMode.LEGACY] behaviour.
     *
     * - The [SourceLocationMeta] will be extracted from [MetaContainer] and included in any [EvaluationException] that
     * is thrown, if present.
     * - The location information is added to the [EvaluationException]'s `errorContext`, if it is not already present.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    override fun handleException(
        sourceLocation: SourceLocationMeta?,
        block: () -> ExprValue
    ): ExprValue =
        try {
            block()
        } catch (e: EvaluationException) {
            // Only add source location data to the error context if it doesn't already exist
            // in [errorContext].
            if (!e.errorContext.hasProperty(Property.LINE_NUMBER)) {
                sourceLocation?.let { fillErrorContext(e.errorContext, sourceLocation) }
            }
            throw e
        } catch (e: Exception) {
            thunkOptions.handleExceptionForLegacyMode(e, sourceLocation)
        }
}

/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions] and for
 * [TypingMode.PERMISSIVE] behaviour.
 */
internal class PermissiveThunkFactory<TEnv>(
    thunkOptions: ThunkOptions,
    valueFactory: ExprValueFactory
) : ThunkFactory<TEnv>(thunkOptions, valueFactory) {

    override fun propagateUnknowns(
        getVal1: () -> ExprValue,
        getVal2: (() -> ExprValue)?,
        getVal3: (() -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue {
        val val1 = getVal1()
        return when (val1.type) {
            ExprValueType.MISSING -> valueFactory.missingValue
            else -> {
                val val2 = getVal2?.let { it() }
                when {
                    val2 == null -> nullOrCompute(val1, null, null, compute)
                    val2.type == ExprValueType.MISSING -> valueFactory.missingValue
                    else -> {
                        val val3 = getVal3?.let { it() }
                        when {
                            val3 == null -> nullOrCompute(val1, val2, null, compute)
                            val3.type == ExprValueType.MISSING -> valueFactory.missingValue
                            else -> nullOrCompute(val1, val2, val3, compute)
                        }
                    }
                }
            }
        }
    }

    override fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue {

        // Because we need to short-circuit on the first MISSING value and [operands] is a sequence,
        // we can't use .map here.  (non-local returns on `.map` are not allowed)
        val argValues = mutableListOf<ExprValue>()
        operands.forEach {
            when (it.type) {
                ExprValueType.MISSING -> return valueFactory.missingValue
                else -> argValues.add(it)
            }
        }
        return when {
            // if any result is `NULL`, propagate return null instead.
            argValues.any { it.type == ExprValueType.NULL } -> valueFactory.nullValue
            else -> compute(argValues)
        }
    }

    private fun nullOrCompute(
        v1: ExprValue,
        v2: ExprValue?,
        v3: ExprValue?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue =
        when {
            v1.type == ExprValueType.NULL ||
                (v2?.let { it.type == ExprValueType.NULL }) ?: false ||
                (v3?.let { it.type == ExprValueType.NULL }) ?: false -> valueFactory.nullValue
            else -> compute(v1, v2, v3)
        }

    /** See [ThunkFactory.thunkFold]. */
    override fun thunkFold(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): Thunk<TEnv> {
        require(argThunks.isNotEmpty()) { "argThunks must not be empty" }

        return thunkEnv(metas) { env ->
            val values = argThunks.map {
                val v = it(env)
                when (v.type) {
                    // Short-circuit at first detected MISSING value.
                    ExprValueType.MISSING -> return@thunkEnv valueFactory.missingValue
                    else -> v
                }
            }
            when {
                // Propagate NULL if any operand is NULL.
                values.any { it.type == ExprValueType.NULL } -> valueFactory.nullValue
                // compute the final value.
                else -> values.reduce { first, second -> op(first, second) }
            }
        }.typeCheck(metas)
    }

    /** See [ThunkFactory.thunkAndMap]. */
    override fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<Thunk<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): Thunk<TEnv> {
        require(argThunks.size >= 2) { "argThunks must have at least two elements" }

        return thunkEnv(metas) thunkBlock@{ env ->
            val values = argThunks.map {
                val v = it(env)
                when (v.type) {
                    // Short-circuit at first detected MISSING value.
                    ExprValueType.MISSING -> return@thunkBlock valueFactory.missingValue
                    else -> v
                }
            }
            when {
                // Propagate NULL if any operand is NULL.
                values.any { it.type == ExprValueType.NULL } -> valueFactory.nullValue
                else -> {
                    (0..(values.size - 2)).forEach { i ->
                        if (!op(values[i], values[i + 1]))
                            return@thunkBlock valueFactory.newBoolean(false)
                    }

                    return@thunkBlock valueFactory.newBoolean(true)
                }
            }
        }
    }

    /**
     * Handles exceptions appropriately for a run-time [Thunk<TEnv>] respecting [TypingMode.PERMISSIVE] behaviour.
     *
     * - Exceptions thrown by [block] that are [EvaluationException] are caught and [MissingExprValue] is returned.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    override fun handleException(
        sourceLocation: SourceLocationMeta?,
        block: () -> ExprValue
    ): ExprValue =
        try {
            block()
        } catch (e: EvaluationException) {
            thunkOptions.handleExceptionForPermissiveMode(e, sourceLocation)
            when (e.errorCode.errorBehaviorInPermissiveMode) {
                // Rethrows the exception as it does in LEGACY mode.
                ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> throw e
                ErrorBehaviorInPermissiveMode.RETURN_MISSING -> valueFactory.missingValue
            }
        } catch (e: Exception) {
            thunkOptions.handleExceptionForLegacyMode(e, sourceLocation)
        }
}
