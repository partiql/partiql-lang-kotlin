package org.partiql.spi.fn

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
    public val type: PartiQLValueType,
)
