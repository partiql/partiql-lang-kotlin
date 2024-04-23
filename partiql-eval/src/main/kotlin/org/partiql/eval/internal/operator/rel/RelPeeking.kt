package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorPeeking
import org.partiql.eval.internal.operator.Operator

/**
 * For [Operator.Relation]'s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [peek] to implement both [hasNext] and [next].
 */
internal abstract class RelPeeking : Operator.Relation, IteratorPeeking<Record>() {

    override fun open(env: Environment) {
        next = null
    }

    override fun close() {
        next = null
    }
}
