package org.partiql.planner.internal.fn

import org.partiql.types.PType

/**
 * Temporary class for function validation to compute return types.
 */
internal object FnValidator {

    /**
     * This computes a PType for a given function.
     */
    @JvmStatic
    fun validate(specific: String, args: List<PType>): PType {
        return PType.typeDynamic()
    }
}
