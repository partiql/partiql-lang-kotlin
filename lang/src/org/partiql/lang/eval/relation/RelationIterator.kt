package org.partiql.lang.eval.relation

enum class RelationType { BAG, LIST }

/**
 * Represents an iterator that is returned by a relational operator during evaluation.
 *
 * This is a "faux" iterator in a sense, because it doesn't provide direct access to a current element.
 *
 * When initially created, the iterator is positioned "before" the first element. [nextRow] should be called to advance
 * the iterator to the first row.
 *
 * We do not use [Iterator] for this purpose because it is not a natural fit.  There are two reasons:
 *
 * 1. [Iterator.next] returns the current element, but this isn't actually an iterator over a collection.   Instead,
 * execution of [nextRow] may have a side effect of populating value(s) in the current
 * [org.partiql.lang.eval.Environment.registers] array.  Bridge operators such as
 * [org.partiql.lang.domains.PartiqlPhysical.Expr.BindingsToValues] are responsible for extracting current values from
 * [org.partiql.lang.eval.Environment.registers] and converting them to the appropriate container [ExprValue]s.
 * 2. [Iterator.hasNext] requires knowing if additional rows remain after the current row, but in the case of a `filter`
 * relation, this requires advancing through possibly all remaining rows to see if any remaining row matches the
 * predicate.
 */
internal interface RelationIterator {
    val relType: RelationType

    /**
     * Advances the iterator to the next row.
     *
     * Returns true to indicate that the next row was found and that [org.partiql.lang.eval.Environment.registers] have
     * been updated for the current row.  False if there are no more rows.
     */
    fun nextRow(): Boolean
}
