package org.partiql.eval.internal.compiler

import java.time.LocalDate

/**
 * Date captured once per query execution.
 *
 * [begin] records the clock at the start of an `execute()` call. Every `CURRENT_DATE` read during
 * that execution observes that date until the next [begin] (the next `execute()` on the same
 * prepared statement).
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
