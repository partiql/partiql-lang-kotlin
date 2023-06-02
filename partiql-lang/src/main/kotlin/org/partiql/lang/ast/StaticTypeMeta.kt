package org.partiql.lang.ast

import org.partiql.types.StaticType

/**
 * Represents a static type for an AST element.
 *
 * Note: The (de)serialization based on ISL mappers does not work as expected but does not break anything since no one uses it today.
 * TODO: issue to track fixing the (de)serialization https://github.com/partiql/partiql-lang-kotlin/issues/512
 */
data class StaticTypeMeta(val type: StaticType) : Meta {

    override fun toString() = type.toString()

    override val tag = TAG

    companion object {
        const val TAG = "\$static_type"
    }
}
