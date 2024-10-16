package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.helpers.IteratorPeeking
import org.partiql.eval.internal.operator.Operator

/**
 * For [Operator.Relation]'s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [peek] to implement both [hasNext] and [next].
 */
internal abstract class RelOpPeeking : Operator.Relation, IteratorPeeking<Row>() {

    /**
     * This shall have the same functionality as [open]. Implementers of [RelOpPeeking] shall not override [open].
     */
    abstract fun openPeeking()

    /**
     * This shall have the same functionality as [close]. Implementers of [RelOpPeeking] shall not override [close].
     */
    abstract fun closePeeking()

    /**
     * Implementers shall not override this method.
     */
    override fun open() {
        next = null
        openPeeking()
    }

    /**
     * Implementers shall not override this method.
     */
    override fun close() {
        next = null
        closePeeking()
    }
}
