package org.partiql.planner.internal.typer

import org.partiql.types.NullType
import org.partiql.types.StaticType

/**
 * In contrast to [set], this accumulates the passed-in [value] to the in-place value corresponding to [key].
 * Note that this does not replace the value corresponding to [key]. The [MutableMap] is acting as a way to
 * accumulate values, and this helper function aids in that.
 */

internal fun MutableMap<PartiQLTyperTestBase.TestResult, Set<List<StaticType>>>.accumulateSuccess(
    key: StaticType,
    value: List<StaticType>,
) {
    val result = PartiQLTyperTestBase.TestResult.Success(key)
    this[result] = setOf(value) + (this[result] ?: emptySet())
}

/**
 * This internally calls [accumulateSuccess], however, this function also checks whether any of the [value] is of type
 * null. If so, it makes sure that the result is also nullable. If the [value] is of type MISSING, the result will
 * be MISSING.
 */
internal fun MutableMap<PartiQLTyperTestBase.TestResult, Set<List<StaticType>>>.accumulateSuccessNullCall(
    key: StaticType,
    value: List<StaticType>,
) {
    val actualKey = when {
        value.any { it is NullType } -> key.asNullable()
        else -> key
    }
    accumulateSuccess(actualKey, value)
}

/**
 * This runs [accumulateSuccessNullCall] over all elements of [value].
 */
internal fun MutableMap<PartiQLTyperTestBase.TestResult, Set<List<StaticType>>>.accumulateSuccesses(
    key: StaticType,
    value: Set<List<StaticType>>,
) {
    value.forEach {
        accumulateSuccessNullCall(key, it)
    }
}
