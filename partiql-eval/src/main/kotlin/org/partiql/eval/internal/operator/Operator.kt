package org.partiql.eval.internal.operator

import org.partiql.eval.internal.Environment
import org.partiql.eval.operator.Record
import org.partiql.spi.value.Datum

/**
 * Operator interfaces for the reference evaluator.
 *
 * TODO rename to avoid naming confusion, but this is all internalized so ok.
 */
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
}
