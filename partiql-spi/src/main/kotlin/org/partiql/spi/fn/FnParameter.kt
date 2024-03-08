package org.partiql.spi.fn

import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Parameter of an [FnSignature].
 *
 * @property name A human-readable name to help clarify its use.
 * @property type The parameter's PartiQL type.
 */
@FnExperimental
@OptIn(PartiQLValueExperimental::class)
public data class FnParameter(
    public val name: String,
    public val type: PartiQLType,
) {
    @Deprecated("Should use the other constructor.")
    public constructor(name: String, type: PartiQLValueType) : this(name, PartiQLType.fromLegacy(type))
}
