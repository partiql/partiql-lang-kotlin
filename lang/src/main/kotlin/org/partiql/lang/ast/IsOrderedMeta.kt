package org.partiql.lang.ast

/**
 * To reduce any extraneous passes over data, this [Meta] indicates whether the associated BindingsToValues Physical
 * expression should be an ordered list or a bag.
 */
object IsOrderedMeta : Meta {
    override val tag = "\$is_ordered"
}
