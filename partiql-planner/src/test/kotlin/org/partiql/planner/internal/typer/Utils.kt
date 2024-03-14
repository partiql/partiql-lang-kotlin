package org.partiql.planner.internal.typer

import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType

/**
 * In contrast to [set], this accumulates the passed-in [value] to the in-place value corresponding to [key].
 * Note that this does not replace the value corresponding to [key]. The [MutableMap] is acting as a way to
 * accumulate values, and this helper function aids in that.
 *
 * This function also checks whether any of the [value] is of type null. If so, it makes sure that the result
 * is also nullable. If the [value] is of type MISSING, the result will be MISSING.
 */
internal fun MutableMap<PartiQLTyperTestBase.TestResult, Set<List<StaticType>>>.accumulateSuccess(
    key: StaticType,
    value: List<StaticType>,
) {
    val actualKey = when {
        value.any { it is MissingType } -> StaticType.MISSING
        value.any { it is NullType } -> key.asNullable()
        else -> key
    }
    val result = PartiQLTyperTestBase.TestResult.Success(actualKey)
    this[result] = setOf(value) + (this[result] ?: emptySet())
}

/**
 * This runs [accumulateSuccess] over all elements of [value].
 */
internal fun MutableMap<PartiQLTyperTestBase.TestResult, Set<List<StaticType>>>.accumulateSuccesses(
    key: StaticType,
    value: Set<List<StaticType>>,
) {
    value.forEach {
        accumulateSuccess(key, it)
    }
}
