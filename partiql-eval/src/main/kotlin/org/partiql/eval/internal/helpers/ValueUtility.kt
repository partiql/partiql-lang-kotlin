package org.partiql.eval.internal.helpers

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Holds helper functions for [PartiQLValue].
 */
internal object ValueUtility {

    /**
     * @return whether the value is a boolean and the value itself is not-null and true.
     */
    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    fun PQLValue.isTrue(): Boolean {
        return this.type == PartiQLValueType.BOOL && !this.isNull && this.boolValue
    }

    @OptIn(PartiQLValueExperimental::class)
    fun PQLValue.check(type: PartiQLValueType): PQLValue {
        if (this.type == type) {
            return this
        }
        if (!this.isNull) {
            throw TypeCheckException()
        }
        return PQLValue.nullValue(type)
    }
}
