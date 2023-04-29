package org.partiql.lang.planner

import org.partiql.types.StaticType

/**
 * Identifies a global variable's type, given its uniqueId.
 *
 * A global variable is usually a database table, but it may also be any other PartiQL value.
 */
fun interface StaticTypeResolver {
    fun getVariableStaticType(uniqueId: String): StaticType
}
