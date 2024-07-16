package org.partiql.planner.internal.fn

import org.partiql.planner.catalog.Routine
import org.partiql.types.PType

/**
 * Class for function validation to compute return types.
 */
internal object FnValidator {

    /**
     * This computes a PType for a given function.
     */
    @JvmStatic
    fun validate(routine: Routine, args: List<PType>): PType {
        return PType.typeDynamic()
    }
}
