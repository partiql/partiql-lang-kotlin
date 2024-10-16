package org.partiql.eval.internal.operator

import org.partiql.eval.internal.Row
import org.partiql.spi.value.Datum

/**
 * TODO make this Java public API in later PR.
 */
internal sealed interface Operator {

    /**
     * Expr represents an evaluable expression tree which returns a value.
     */
    interface Expr : Operator {

        fun eval(): Datum
    }

    /**
     * Relation operator represents an evaluable collection of binding tuples.
     */
    interface Relation : Operator, AutoCloseable, Iterator<Row> {

        fun open()

        override fun close()
    }

    interface Aggregation : Operator {

        val delegate: org.partiql.spi.function.Aggregation

        val args: List<Expr>

        val setQuantifier: SetQuantifier

        enum class SetQuantifier {
            ALL,
            DISTINCT
        }
    }
}
