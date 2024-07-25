package org.partiql.eval.internal.operator

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental

internal sealed interface Operator {

    /**
     * Expr represents an evaluable expression tree which returns a value.
     */
    interface Expr : Operator {

        fun eval(env: Environment): Datum
    }

    /**
     * Relation operator represents an evaluable collection of binding tuples.
     */
    interface Relation : Operator, AutoCloseable, Iterator<Record> {

        fun open(env: Environment)

        override fun close()
    }

    interface Aggregation : Operator {

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
