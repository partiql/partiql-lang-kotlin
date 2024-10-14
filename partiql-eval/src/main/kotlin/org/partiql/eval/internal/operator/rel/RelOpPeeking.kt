package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.IteratorPeeking
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.operator.Record

/**
 * For [Operator.Relation]'s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [peek] to implement both [hasNext] and [next].
 */
internal abstract class RelOpPeeking : Operator.Relation, IteratorPeeking<Record>() {

    /**
     * This shall have the same functionality as [open]. Implementers of [RelOpPeeking] shall not override [open].
     */
    abstract fun openPeeking(env: Environment)

    /**
     * This shall have the same functionality as [close]. Implementers of [RelOpPeeking] shall not override [close].
     */
    abstract fun closePeeking()

    /**
     * Implementers shall not override this method.
     */
    override fun open(env: Environment) {
        next = null
        openPeeking(env)
    }

    /**
     * Implementers shall not override this method.
     */
    override fun close() {
        next = null
        closePeeking()
    }
}
