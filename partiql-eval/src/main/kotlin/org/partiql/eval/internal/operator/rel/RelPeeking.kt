package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

/**
 * For [Operator.Relation]'s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [peek] to implement both [hasNext] and [next].
 */
internal abstract class RelPeeking : Operator.Relation {

    private var _next: Record? = null

    /**
     * @return Null when there is not another record to be produced. Returns a [Record] when able to.
     *
     * @see RelPeeking
     */
    abstract fun peek(): Record?

    override fun open(env: Environment) {
        _next = null
    }

    override fun hasNext(): Boolean {
        if (_next != null) {
            return true
        }
        this._next = peek()
        return this._next != null
    }

    override fun next(): Record {
        val next = _next
            ?: peek()
            ?: error("There was not a record to be produced, however, next() was called. Please use hasNext() beforehand.")
        this._next = null
        return next
    }

    override fun close() {
        _next = null
    }
}
