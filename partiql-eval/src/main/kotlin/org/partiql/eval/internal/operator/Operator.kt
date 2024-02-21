package org.partiql.eval.internal.operator

import org.partiql.eval.internal.Record
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal sealed interface Operator {

    /**
     * Expr represents an evaluable expression tree which returns a value.
     */
    interface Expr : Operator {

        @OptIn(PartiQLValueExperimental::class)
        fun eval(record: Record): PartiQLValue
    }

    /**
     * Relation operator represents an evaluable collection of binding tuples.
     */
    interface Relation : Operator, AutoCloseable {

        fun open()

        fun next(): Record?

        override fun close()
    }

    interface Accumulator : Operator {

        @OptIn(FnExperimental::class)
        val delegate: Agg

        val args: List<Expr>

        val setQuantifier: SetQuantifier

        enum class SetQuantifier {
            ALL,
            DISTINCT
        }
    }
}
