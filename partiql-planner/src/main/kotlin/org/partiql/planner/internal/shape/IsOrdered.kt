package org.partiql.planner.internal.shape

import org.partiql.shape.Meta

/**
 * This can only be added to a [org.partiql.shape.PShape] that is of type [org.partiql.value.TupleType].
 *
 * The presence of this in a [org.partiql.shape.PShape.metas] describes the fact that the TUPLE is ordered. This is
 * specifically for type inference.
 */
internal object IsOrdered : Meta.Base() {
    override fun toString(): String = "IS ORDERED"
}
