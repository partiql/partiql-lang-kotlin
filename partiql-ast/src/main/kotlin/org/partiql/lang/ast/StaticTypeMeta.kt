package org.partiql.lang.ast

import org.partiql.types.StaticType

/**
 * Represents a static type for an AST element.
 */
data class StaticTypeMeta(val type: StaticType) : Meta {

    override fun toString() = type.toString()

    override val tag = TAG

    companion object {
        const val TAG = "\$static_type"
    }
}
