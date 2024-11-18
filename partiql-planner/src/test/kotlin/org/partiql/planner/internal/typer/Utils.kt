package org.partiql.planner.internal.typer

import org.partiql.types.PType

/**
 * In contrast to [set], this accumulates the passed-in [value] to the in-place value corresponding to [key].
 * Note that this does not replace the value corresponding to [key]. The [MutableMap] is acting as a way to
 * accumulate values, and this helper function aids in that.
 */
internal fun <T> MutableMap<PartiQLTyperTestBase.TestResult, Set<List<T>>>.accumulateSuccess(
    key: PType,
    value: List<T>,
) {
    val result = PartiQLTyperTestBase.TestResult.Success(key)
    this[result] = setOf(value) + (this[result] ?: emptySet())
}

/**
 * This runs [accumulateSuccess] over all elements of [value].
 */
internal fun <T> MutableMap<PartiQLTyperTestBase.TestResult, Set<List<T>>>.accumulateSuccesses(
    key: PType,
    value: Set<List<T>>,
) {
    value.forEach {
        accumulateSuccess(key, it)
    }
}
