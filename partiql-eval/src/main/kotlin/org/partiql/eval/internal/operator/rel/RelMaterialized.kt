package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

/**
 * For [Operator.Relation]'s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [materializeNext] to implement both [hasNext] and [next].
 */
internal abstract class RelMaterialized : Operator.Relation {

    private var _nextIsReady: Boolean = false
    private lateinit var _next: Record

    /**
     * @return Null when there is not another record to be produced. Returns a [Record] when able to.
     *
     * @see RelMaterialized
     */
    abstract fun materializeNext(): Record?

    override fun open(env: Environment) {
        _nextIsReady = false
    }

    override fun hasNext(): Boolean {
        if (_nextIsReady) {
            return true
        }
        this._next = materializeNext() ?: return false
        _nextIsReady = true
        return true
    }

    override fun next(): Record {
        if (!_nextIsReady) {
            _next = materializeNext()
                ?: error("There was not a record to be produced, however, next() was called. Please use hasNext() beforehand.")
        }
        _nextIsReady = false
        return _next
    }

    override fun close() {
        _nextIsReady = false
    }
}
