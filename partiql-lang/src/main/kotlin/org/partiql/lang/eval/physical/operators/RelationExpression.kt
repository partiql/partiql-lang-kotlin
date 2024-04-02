package org.partiql.lang.eval.physical.operators

import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator

/**
 * An implementation of a physical plan relational operator.
 *
 * PartiQL's relational algebra is based on
 * [E.F. Codd's Relational Algebra](https://en.wikipedia.org/wiki/Relational_algebra), but to better support
 * semi-structured, schemaless data, our "relations" are actually logical collections of bindings.  Still, the term
 * "relation" has remained, as well as most other concepts from E.F. Codd's relational algebra.
 *
 * Like [ValueExpression], this is public API that is supported long term and is intended to avoid exposing
 * implementation details such as [org.partiql.lang.eval.physical.RelationThunkEnv].
 */
fun interface RelationExpression {
    fun evaluate(state: EvaluatorState): RelationIterator
}
