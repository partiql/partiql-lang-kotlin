package org.partiql.lang.util

import org.partiql.lang.eval.EvaluatingCompiler

internal inline fun <T> compareBy(
    ordering: EvaluatingCompiler.OrderingType,
    nulls: EvaluatingCompiler.NullsType,
    crossinline selector: (T) -> Comparable<*>?
): Comparator<T> =
    Comparator { a, b ->
        val l = selector(a)
        val r = selector(b)
        when {
            l === r -> 0
            l == null -> if (nulls == EvaluatingCompiler.NullsType.FIRST) -1 else 1
            r == null -> if (nulls == EvaluatingCompiler.NullsType.LAST) -1 else 1
            else -> when (ordering) {
                EvaluatingCompiler.OrderingType.ASC -> compareValues(l, r)
                EvaluatingCompiler.OrderingType.DESC -> compareValues(r, l)
            }
        }
    }

internal inline fun <T> Comparator<T>.thenBy(
    ordering: EvaluatingCompiler.OrderingType,
    nulls: EvaluatingCompiler.NullsType,
    crossinline selector: (T) -> Comparable<*>?
): Comparator<T> =
    Comparator { a, b ->
        val previousCompare = this@thenBy.compare(a, b)
        if (previousCompare != 0) previousCompare
        else {
            val l = selector(a)
            val r = selector(b)
            when {
                l === r -> 0
                l == null -> if (nulls == EvaluatingCompiler.NullsType.FIRST) -1 else 1
                r == null -> if (nulls == EvaluatingCompiler.NullsType.LAST) -1 else 1
                else -> when (ordering) {
                    EvaluatingCompiler.OrderingType.ASC -> compareValues(l, r)
                    EvaluatingCompiler.OrderingType.DESC -> compareValues(r, l)
                }
            }
        }
    }
