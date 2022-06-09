package org.partiql.lang.eval.physical.operators

import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator

/**
 * An implementation of a relational operator (a.k.a bindings expression).
 *
 * Like [ValueExpr], this is public API that is supported long term and is intended to avoid exposing implementation
 * details such as [org.partiql.lang.eval.physical.RelationThunkEnv].
 */
fun interface BindingsExpr {
    operator fun invoke(state: EvaluatorState): RelationIterator
}
