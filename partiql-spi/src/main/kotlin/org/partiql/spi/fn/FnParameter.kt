package org.partiql.spi.fn

import org.partiql.types.PType

/**
 * Parameter of an [FnSignature].
 *
 * @property name A human-readable name to help clarify its use.
 * @property type The parameter's PartiQL type.
 */
public data class FnParameter(
    public val name: String,
    public val type: PType,
)
