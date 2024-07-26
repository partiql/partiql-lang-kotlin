package org.partiql.planner.internal.fn

import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Parameter of an [FnSignature].
 *
 * @property name A human-readable name to help clarify its use.
 * @property type The parameter's PartiQL type.
 */
@OptIn(PartiQLValueExperimental::class)
public data class FnParameter(
    public val name: String,
    public val type: PType,
) {
    public constructor(
        name: String,
        type: PartiQLValueType,
    ) : this(name, PType.fromPartiQLValueType(type))
}
