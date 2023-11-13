package org.partiql.lang.eval.relation

import org.partiql.lang.domains.PartiqlPhysical.Expr.BindingsToValues
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState

enum class RelationType { BAG, LIST }

/**
 * Represents an iterator that is returned by a relational operator during evaluation.
 *
 * This is a "faux" iterator in a sense, because it doesn't provide direct access to a current element.
 *
 * When initially created, the [RelationIterator] is positioned "before" the first element. [nextRow] should be called
 * to advance the iterator to the first row.
 *
 * We do not use [Iterator] for this purpose because it is not a natural fit.  There are two reasons:
 *
 * 1. [Iterator.next] returns the current element, but this isn't actually an iterator over a collection.   Instead,
 * execution of [nextRow] may have a side effect of populating value(s) in the current [EvaluatorState.registers]
 * array.  Bridge operators such as [BindingsToValues] are responsible for extracting current values from
 * [EvaluatorState.registers] and converting them to the appropriate container [ExprValue]s.  In this sense,
 * PartiQL's "relations" are actually logical collections of variable bindings.  This differs from how the term
 * is defined by [E. F. Codd's relational algebra](https://en.wikipedia.org/wiki/Relational_algebra).
 * 2. [Iterator.hasNext] would require knowing if additional rows remain after the current row, but in a few cases
 * including filters and joins that requires advancing through possibly all remaining rows to see if any remaining row
 * matches the predicate.  This is awkward to implement and would force eager evaluation of the [Iterator].
 */
interface RelationIterator {
    val relType: RelationType

    /**
     * Advances the iterator to the next row.
     *
     * Returns true to indicate that the next row was found and that [EvaluatorState.registers] have been updated for
     * the current row.  False if there are no more rows.
     */
    fun nextRow(): Boolean
}
