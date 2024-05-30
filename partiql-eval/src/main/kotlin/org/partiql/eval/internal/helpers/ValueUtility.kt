package org.partiql.eval.internal.helpers

import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Holds helper functions for [PartiQLValue].
 */
internal object ValueUtility {

    /**
     * @return whether the value is a boolean and the value itself is not-null and true.
     */
    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    fun PartiQLValue.isTrue(): Boolean {
        return this is BoolValue && this.value == true
    }
}
