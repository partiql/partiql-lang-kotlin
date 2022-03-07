package org.partiql.lang.eval.relation

enum class RelationType { BAG, LIST }

/**
 * Represents an iterator that is returned by a relational operator during evaluation.
 *
 * This is a "faux" iterator in a sense, because it doesn't provide direct access to a current element.  Instead,
 * execution of [nextRow] may have a side effect of populating one or more values in the current [Environment.registers]
 * array.  Bridge operators such as [org.partiql.lang.domains.PartiqlPhysical.Expr.BindingsToValues] are responsible
 * for extracting current values from [Environment.registers] and converting them to the appropriate container
 * [ExprValue]s.
 *
 * When initially created, the iterator is positioned "before" the first element. [nextRow] should be called to advance
 * the iterator to the first row.
 *
 * We do not use have an equivalent of [Iterator.hasNext] because this doesn't really work well for our use case.  For
 * instance, when iterating over the results of a filtered collection, it is necessary to advance though possibly all
 * of the remaining items in the collection before knowing if another row exists or not, and that leads to tricky state
 * management.  Instead [nextRow] advances to the next row in the relation and returns a [Boolean] `true` when a row
 * was read, `false` when no additional rows were found.
 */
internal interface RelationIterator {
    val relType: RelationType

    /**
     * Advances the iterator to the next row.
     *
     * Returns true to indicate that the next row was found and that [Environment.registers] have been updated
     * for the current row.  False if there are no more rows.
     */
    fun nextRow(): Boolean
}

