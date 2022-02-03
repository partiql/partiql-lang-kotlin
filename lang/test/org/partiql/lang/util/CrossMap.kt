package org.partiql.lang.util

/** Performs a functional map of the cross-product of [l1] and [l2]. */
fun <T1, T2, R> crossMap(l1: List<T1>, l2: List<T2>, block: (T1, T2) -> R): List<R> =
    l1.map { left ->
        l2.map { right ->
            block(left, right)
        }
    }.flatten()

/** Performs a functional map of the cross-product of [l1], [l2] and [l3]. */
fun <T1, T2, T3, R> crossMap(l1: List<T1>, l2: List<T2>, l3: List<T3>, block: (T1, T2, T3) -> R): List<R> =
    l1.map { v1 ->
        l2.map { v2 ->
            l3.map { v3 ->
                block(v1, v2, v3)
            }
        }.flatten()
    }.flatten()