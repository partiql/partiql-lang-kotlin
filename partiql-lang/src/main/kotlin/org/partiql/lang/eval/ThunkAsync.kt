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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.partiql.errors.ErrorBehaviorInPermissiveMode
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.staticType
import org.partiql.lang.types.StaticTypeUtils.isInstance

/**
 * A thunk with no parameters other than the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * @param TEnv The type of the environment.  Generic so that the legacy AST compiler and the new compiler may use
 * different types here.
 */
internal typealias ThunkAsync<TEnv> = suspend (TEnv) -> ExprValue

/**
 * A thunk taking a single argument and the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * @param TEnv The type of the environment.  Generic so that the legacy AST compiler and the new compiler may use
 * different types here.
 * @param TArg The type of the additional argument.
 */
internal typealias ThunkValueAsync<TEnv, TArg> = suspend (TEnv, TArg) -> ExprValue

/**
 * An extension method for creating [ThunkFactoryAsync] based on the type of [TypingMode]
 *  - when [TypingMode] is [TypingMode.LEGACY], creates [LegacyThunkFactoryAsync]
 *  - when [TypingMode] is [TypingMode.PERMISSIVE], creates [PermissiveThunkFactoryAsync]
 */
internal fun <TEnv> TypingMode.createThunkFactoryAsync(
    thunkOptions: ThunkOptions
): ThunkFactoryAsync<TEnv> = when (this) {
    TypingMode.LEGACY -> LegacyThunkFactoryAsync(thunkOptions)
    TypingMode.PERMISSIVE -> PermissiveThunkFactoryAsync(thunkOptions)
}
/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions].
 */
internal abstract class ThunkFactoryAsync<TEnv>(
    val thunkOptions: ThunkOptions
) {
    private fun checkEvaluationTimeType(thunkResult: ExprValue, metas: MetaContainer): ExprValue {
        // When this check is enabled we throw an exception the [MetaContainer] does not have a
        // [StaticTypeMeta].  This indicates a bug or unimplemented support for an AST node in
        // [StaticTypeInferenceVisitorTransform].
        val staticType = metas.staticType?.type ?: error("Metas collection does not have a StaticTypeMeta")
        if (!isInstance(thunkResult, staticType)) {
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
     * confusion in the case [org.partiql.lang.eval.visitors.StaticTypeInferenceVisitorTransform] has a bug which
     * prevents it from assigning a [StaticTypeMeta] or in case it is not run at all.
     */
    protected suspend fun ThunkAsync<TEnv>.typeCheck(metas: MetaContainer): ThunkAsync<TEnv> =
        when (thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> this
            ThunkReturnTypeAssertions.ENABLED -> {
                val wrapper: ThunkAsync<TEnv> = { env: TEnv ->
                    val thunkResult: ExprValue = this(env)
                    checkEvaluationTimeType(thunkResult, metas)
                }
                wrapper
            }
        }

    /** Same as [typeCheck] but works on a [ThunkEnvValue<ExprValue>] instead of a [Thunk<TEnv>]. */
    protected suspend fun ThunkValueAsync<TEnv, ExprValue>.typeCheckEnvValue(metas: MetaContainer): ThunkValueAsync<TEnv, ExprValue> =
        when (thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> this
            ThunkReturnTypeAssertions.ENABLED -> {
                val wrapper: ThunkValueAsync<TEnv, ExprValue> = { env: TEnv, value: ExprValue ->
                    val thunkResult: ExprValue = this(env, value)
                    checkEvaluationTimeType(thunkResult, metas)
                }
                wrapper
            }
        }

    /**
     * Creates a [Thunk<TEnv>] which handles exceptions by wrapping them into an [EvaluationException] which uses
     * [handleExceptionAsync] to handle exceptions appropriately.
     *
     * Literal lambdas passed to this function as [t] are inlined into the body of the function being returned, which
     * reduces the need to create additional call contexts.  The lambdas passed as [t] may not contain non-local returns
     * (`crossinline`).
     */
    internal suspend inline fun thunkEnvAsync(metas: MetaContainer, crossinline t: ThunkAsync<TEnv>): ThunkAsync<TEnv> {
        val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta

        val thunkAsync: ThunkAsync<TEnv> = { env: TEnv ->
            this.handleExceptionAsync(sourceLocationMeta) {
                t(env)
            }
        }
        return thunkAsync.typeCheck(metas)
    }

    /**
     * Defines the strategy for unknown propagation of 1-3 operands.
     *
     * This is the [TypingMode] specific implementation of unknown-propagation, used by the [thunkEnvOperands]
     * functions.  [getVal1], [getVal2] and [getVal2] are lambdas to allow for differences in short-circuiting.
     *
     * For all [TypingMode]s, if the values returned by [getVal1], [getVal2] and [getVal2] are all known,
     * [compute] is invoked to perform the operation-specific computation.
     *
     * Note: this must be public due to a Kotlin compiler bug: https://youtrack.jetbrains.com/issue/KT-22625.
     * This shouldn't matter though because this class is still `internal`.
     */
    abstract suspend fun propagateUnknowns(
        getVal1: suspend () -> ExprValue,
        getVal2: (suspend () -> ExprValue)?,
        getVal3: (suspend () -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue

    /**
     * Similar to the other [propagateUnknowns] overload, performs unknown propagation for a variadic sequence of
     * operations.
     *
     * Note: this must be public due to a Kotlin compiler bug: https://youtrack.jetbrains.com/issue/KT-22625.
     * This shouldn't matter though because this class is still `internal`.
     */
    abstract suspend fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue

    /**
     * Creates a thunk that accepts three [Thunk<TEnv>] operands ([t1], [t2], and [t3]), evaluates them and propagates
     * unknowns according to the current [TypingMode].  When possible, use this function or one of its overloads
     * instead of [thunkEnvAsync] when the operation requires propagation of unknown values.
     *
     * [t1], [t2] and [t3] are each evaluated in with short-circuiting depending on the current [TypingMode]:
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
    internal suspend inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: ThunkAsync<TEnv>,
        crossinline t2: ThunkAsync<TEnv>,
        crossinline t3: ThunkAsync<TEnv>,
        crossinline compute: (TEnv, ExprValue, ExprValue, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv> =
        this.thunkEnvAsync(metas) { env ->
            propagateUnknowns({ t1(env) }, { t2(env) }, { t3(env) }) { v1, v2, v3 ->
                compute(env, v1, v2!!, v3!!)
            }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with three [Thunk<TEnv>] operands. */
    internal suspend inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: ThunkAsync<TEnv>,
        crossinline t2: ThunkAsync<TEnv>,
        crossinline compute: (TEnv, ExprValue, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv> =
        this.thunkEnvAsync(metas) { env ->
            propagateUnknowns({ t1(env) }, { t2(env) }, null) { v1, v2, _ ->
            compute(env, v1, v2!!)
        }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with three [Thunk<TEnv>] operands. */
    internal suspend inline fun thunkEnvOperands(
        metas: MetaContainer,
        crossinline t1: ThunkAsync<TEnv>,
        crossinline compute: (TEnv, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv> =
        this.thunkEnvAsync(metas) { env ->
            propagateUnknowns({ t1(env) }, null, null) { v1, _, _ ->
            compute(env, v1)
        }
        }.typeCheck(metas)

    /** See the [thunkEnvOperands] with a variadic list of [Thunk<TEnv>] operands. */
    internal suspend inline fun thunkEnvOperands(
        metas: MetaContainer,
        operandThunks: List<ThunkAsync<TEnv>>,
        crossinline compute: (TEnv, List<ExprValue>) -> ExprValue
    ): ThunkAsync<TEnv> {

        return this.thunkEnvAsync(metas) { env ->
            val operandSeq = flow {
                operandThunks.forEach { emit(it(env)) }
            }
            propagateUnknowns(operandSeq.toList().asSequence()) { values ->
                compute(env, values)
            }
        }.typeCheck(metas)
    }

    /** Similar to [thunkEnvAsync], but creates a [ThunkEnvValue<ExprValue>] instead. */
    internal suspend inline fun thunkEnvValue(
        metas: MetaContainer,
        crossinline t: ThunkValueAsync<TEnv, ExprValue>
    ): ThunkValueAsync<TEnv, ExprValue> {
        val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta

        val tVal: ThunkValueAsync<TEnv, ExprValue> = { env: TEnv, arg1: ExprValue ->
            this.handleExceptionAsync(sourceLocationMeta) {
                t(env, arg1)
            }
        }
        return tVal.typeCheckEnvValue(metas)
    }

    /**
     * Similar to [thunkEnvAsync] but evaluates all [argThunks] and performs a fold using [op] as the operation.
     *
     * Also handles null propagation appropriately for NAryOp arithmetic operations.  Each thunk in [argThunks]
     * is evaluated in turn and:
     *
     * - for [TypingMode.LEGACY], the first unknown operand short-circuits, returning `NULL`.
     * - for [TypingMode.PERMISSIVE], the first missing operand short-circuits, returning `MISSING`.  Then, if one
     * of the operands returned `NULL`, `NULL` is returned.
     *
     * For both modes, if all the operands are known, performs a fold over them with [op].
     */
    internal abstract suspend fun thunkFold(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv>

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
     * If [op] is true for all invocations then the result of the thunk becomes `true`, otherwise the result is `false`.
     *
     * The name of this function was inspired by Racket's `andmap` procedure.
     */
    internal abstract suspend fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): ThunkAsync<TEnv>

    /** Populates [exception] with the line & column from the specified [SourceLocationMeta]. */
    protected fun populateErrorContext(
        exception: EvaluationException,
        sourceLocation: SourceLocationMeta?
    ): EvaluationException {
        // Only add source location data to the error context if it doesn't already exist
        // in [errorContext].
        if (!exception.errorContext.hasProperty(Property.LINE_NUMBER)) {
            sourceLocation?.let { fillErrorContext(exception.errorContext, sourceLocation) }
        }
        return exception
    }

    /**
     * Handles exceptions appropriately for a run-time [ThunkAsync<TEnv>].
     *
     * - The [SourceLocationMeta] will be extracted from [MetaContainer] and included in any [EvaluationException] that
     * is thrown, if present.
     * - The location information is added to the [EvaluationException]'s `errorContext`, if it is not already present.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    abstract suspend fun handleExceptionAsync(
        sourceLocation: SourceLocationMeta?,
        block: suspend () -> ExprValue
    ): ExprValue
}

/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions] for [TypingMode.LEGACY] behaviour.
 */
internal class LegacyThunkFactoryAsync<TEnv>(
    thunkOptions: ThunkOptions
) : ThunkFactoryAsync<TEnv>(thunkOptions) {

    override suspend fun propagateUnknowns(
        getVal1: suspend () -> ExprValue,
        getVal2: (suspend () -> ExprValue)?,
        getVal3: (suspend () -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue {
        val val1 = getVal1()
        return when {
            val1.isUnknown() -> ExprValue.nullValue
            else -> {
                val val2 = getVal2?.let { it() }
                when {
                    val2 == null -> compute(val1, null, null)
                    val2.isUnknown() -> ExprValue.nullValue
                    else -> {
                        val val3 = getVal3?.let { it() }
                        when {
                            val3 == null -> compute(val1, val2, null)
                            val3.isUnknown() -> ExprValue.nullValue
                            else -> compute(val1, val2, val3)
                        }
                    }
                }
            }
        }
    }

    override suspend fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue {
        // Because we need to short-circuit on the first unknown value and [operands] is a sequence,
        // we can't use .map here.  (non-local returns on `.map` are not allowed)
        val argValues = mutableListOf<ExprValue>()
        operands.forEach {
            when {
                it.isUnknown() -> return ExprValue.nullValue
                else -> argValues.add(it)
            }
        }
        return compute(argValues)
    }

    /** See [ThunkFactoryAsync.thunkFold]. */
    override suspend fun thunkFold(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv> {
        require(argThunks.isNotEmpty()) { "argThunks must not be empty" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)
        return thunkEnvAsync(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                // Short-circuit at first NULL or MISSING value and return NULL.
                firstValue.isUnknown() -> ExprValue.nullValue
                else -> {
                    otherThunks.fold(firstValue) { acc, curr ->
                        val currValue = curr(env)
                        if (currValue.type.isUnknown) {
                            return@thunkBlock ExprValue.nullValue
                        }
                        op(acc, currValue)
                    }
                }
            }
        }.typeCheck(metas)
    }

    /** See [ThunkFactoryAsync.thunkAndMap]. */
    override suspend fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): ThunkAsync<TEnv> {
        require(argThunks.size >= 2) { "argThunks must have at least two elements" }

        val firstThunk = argThunks.first()
        val otherThunks = argThunks.drop(1)

        return thunkEnvAsync(metas) thunkBlock@{ env ->
            val firstValue = firstThunk(env)
            when {
                // If the first value is unknown, short circuit returning null.
                firstValue.isUnknown() -> ExprValue.nullValue
                else -> {
                    otherThunks.fold(firstValue) { lastValue, currentThunk ->

                        val currentValue = currentThunk(env)
                        if (currentValue.isUnknown()) {
                            return@thunkBlock ExprValue.nullValue
                        }

                        val result = op(lastValue, currentValue)
                        if (!result) {
                            return@thunkBlock ExprValue.newBoolean(false)
                        }

                        currentValue
                    }

                    ExprValue.newBoolean(true)
                }
            }
        }
    }

    /**
     * Handles exceptions appropriately for a run-time [ThunkAsync<TEnv>] respecting [TypingMode.LEGACY] behaviour.
     *
     * - The [SourceLocationMeta] will be extracted from [MetaContainer] and included in any [EvaluationException] that
     * is thrown, if present.
     * - The location information is added to the [EvaluationException]'s `errorContext`, if it is not already present.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    override suspend fun handleExceptionAsync(
        sourceLocation: SourceLocationMeta?,
        block: suspend () -> ExprValue
    ): ExprValue =
        try {
            block()
        } catch (e: EvaluationException) {
            throw populateErrorContext(e, sourceLocation)
        } catch (e: Exception) {
            thunkOptions.handleExceptionForLegacyMode(e, sourceLocation)
        }
}

/**
 * Provides methods for constructing new thunks according to the specified [CompileOptions] and for
 * [TypingMode.PERMISSIVE] behaviour.
 */
internal class PermissiveThunkFactoryAsync<TEnv>(
    thunkOptions: ThunkOptions
) : ThunkFactoryAsync<TEnv>(thunkOptions) {

    override suspend fun propagateUnknowns(
        getVal1: suspend () -> ExprValue,
        getVal2: (suspend () -> ExprValue)?,
        getVal3: (suspend () -> ExprValue)?,
        compute: (ExprValue, ExprValue?, ExprValue?) -> ExprValue
    ): ExprValue {
        val val1 = getVal1()
        return when (val1.type) {
            ExprValueType.MISSING -> ExprValue.missingValue
            else -> {
                val val2 = getVal2?.let { it() }
                when {
                    val2 == null -> nullOrCompute(val1, null, null, compute)
                    val2.type == ExprValueType.MISSING -> ExprValue.missingValue
                    else -> {
                        val val3 = getVal3?.let { it() }
                        when {
                            val3 == null -> nullOrCompute(val1, val2, null, compute)
                            val3.type == ExprValueType.MISSING -> ExprValue.missingValue
                            else -> nullOrCompute(val1, val2, val3, compute)
                        }
                    }
                }
            }
        }
    }

    override suspend fun propagateUnknowns(
        operands: Sequence<ExprValue>,
        compute: (List<ExprValue>) -> ExprValue
    ): ExprValue {

        // Because we need to short-circuit on the first MISSING value and [operands] is a sequence,
        // we can't use .map here.  (non-local returns on `.map` are not allowed)
        val argValues = mutableListOf<ExprValue>()
        operands.forEach {
            when (it.type) {
                ExprValueType.MISSING -> return ExprValue.missingValue
                else -> argValues.add(it)
            }
        }
        return when {
            // if any result is `NULL`, propagate return null instead.
            argValues.any { it.type == ExprValueType.NULL } -> ExprValue.nullValue
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
                (v3?.let { it.type == ExprValueType.NULL }) ?: false -> ExprValue.nullValue
            else -> compute(v1, v2, v3)
        }

    /** See [ThunkFactoryAsync.thunkFold]. */
    override suspend fun thunkFold(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> ExprValue
    ): ThunkAsync<TEnv> {
        require(argThunks.isNotEmpty()) { "argThunks must not be empty" }

        return thunkEnvAsync(metas) { env ->
            val values = argThunks.map {
                val v = it(env)
                when (v.type) {
                    // Short-circuit at first detected MISSING value.
                    ExprValueType.MISSING -> return@thunkEnvAsync ExprValue.missingValue
                    else -> v
                }
            }
            when {
                // Propagate NULL if any operand is NULL.
                values.any { it.type == ExprValueType.NULL } -> ExprValue.nullValue
                // compute the final value.
                else -> values.reduce { first, second -> op(first, second) }
            }
        }.typeCheck(metas)
    }

    /** See [ThunkFactoryAsync.thunkAndMap]. */
    override suspend fun thunkAndMap(
        metas: MetaContainer,
        argThunks: List<ThunkAsync<TEnv>>,
        op: (ExprValue, ExprValue) -> Boolean
    ): ThunkAsync<TEnv> {
        require(argThunks.size >= 2) { "argThunks must have at least two elements" }

        return thunkEnvAsync(metas) thunkBlock@{ env ->
            val values = argThunks.map {
                val v = it(env)
                when (v.type) {
                    // Short-circuit at first detected MISSING value.
                    ExprValueType.MISSING -> return@thunkBlock ExprValue.missingValue
                    else -> v
                }
            }
            when {
                // Propagate NULL if any operand is NULL.
                values.any { it.type == ExprValueType.NULL } -> ExprValue.nullValue
                else -> {
                    (0..(values.size - 2)).forEach { i ->
                        if (!op(values[i], values[i + 1]))
                            return@thunkBlock ExprValue.newBoolean(false)
                    }

                    return@thunkBlock ExprValue.newBoolean(true)
                }
            }
        }
    }

    /**
     * Handles exceptions appropriately for a run-time [Thunk<TEnv>] respecting [TypingMode.PERMISSIVE] behaviour.
     *
     * - Exceptions thrown by [block] that are [EvaluationException] are caught and [ExprValue.missingValue] is returned.
     * - Exceptions thrown by [block] that are not an [EvaluationException] cause an [EvaluationException] to be thrown
     * with the original exception as the cause.
     */
    override suspend fun handleExceptionAsync(
        sourceLocation: SourceLocationMeta?,
        block: suspend () -> ExprValue
    ): ExprValue =
        try {
            block()
        } catch (e: EvaluationException) {
            thunkOptions.handleExceptionForPermissiveMode(e, sourceLocation)
            when (e.errorCode.errorBehaviorInPermissiveMode) {
                // Rethrows the exception as it does in LEGACY mode.
                ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> throw populateErrorContext(e, sourceLocation)
                ErrorBehaviorInPermissiveMode.RETURN_MISSING -> ExprValue.missingValue
            }
        } catch (e: Exception) {
            thunkOptions.handleExceptionForLegacyMode(e, sourceLocation)
        }
}
