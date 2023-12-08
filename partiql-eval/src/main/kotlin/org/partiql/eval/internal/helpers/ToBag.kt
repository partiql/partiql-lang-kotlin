package org.partiql.eval.internal.helpers

import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Coercion function F for bag operators described in RFC-0007
 *  - F(absent_value) -> << >>
 *  - F(scalar_value) -> << scalar_value >> # singleton bag
 *  - F(tuple_value)  -> << tuple_value >>  # singleton bag, see future extensions
 *  - F(array_value)  -> bag_value          # discard ordering
 *  - F(bag_value)    -> bag_value          # identity
 */
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValue.toBag(): BagValue<*> {
    TODO("For OUTER set operators")
}
