package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Invoke the constructor over all inputs.
 *
 * @property input
 * @property constructor
 */
internal class ExprSelect(
    private val env: Environment,
    private val input: Operator.Relation,
    private val constructor: Operator.Expr,
    private val ordered: Boolean,
) : Operator.Expr {

    /**
     * An iterator implementation
     */
    private val elements = object : Iterator<Datum> {

        private var _init = false

        override fun hasNext(): Boolean {
            if (!_init) {
                input.open()
                _init = true
            }
            val hasNext = input.hasNext()
            if (!hasNext) {
                input.close()
            }
            return hasNext
        }

        override fun next(): Datum {
            val row = input.next()
            return env.scope(row) { constructor.eval() }
        }
    }

    /**
     * Return a datum backed by the iterator (provided behind an [Iterable])
     */
    override fun eval(): Datum = when (ordered) {
        true -> Datum.list(Iterable { elements })
        false -> Datum.bag(Iterable { elements })
    }
}
