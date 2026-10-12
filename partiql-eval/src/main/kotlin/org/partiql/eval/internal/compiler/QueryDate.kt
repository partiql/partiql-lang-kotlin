package org.partiql.eval.internal.compiler

import java.time.LocalDate

/**
 * Date captured once per query execution.
 *
 * [begin] records the clock at the start of execution. Later reads, including lazy results
 * materialized after execution returns, observe that same date until the next [begin].
 */
internal class QueryDate(private val clock: () -> LocalDate) {

    private var captured: LocalDate? = null

    fun begin() {
        captured = clock()
    }

    fun date(): LocalDate {
        val existing = captured
        if (existing != null) {
            return existing
        }
        val now = clock()
        captured = now
        return now
    }
}
