package org.partiql.types.function

import org.partiql.types.PartiQLValueType

/**
 * This currently only contains [ValueParameter], however, this is designed in this
 * manner to allow for other types of parameters in the future. TypeParameter may be
 * of use.
 */
public sealed class FunctionParameter {

    /**
     * Represents a value parameter.
     *
     * @property name A human-readable name to help clarify its use.
     * @property type The parameter's PartiQL type.
     */
    public class ValueParameter(
        public val name: String,
        public val type: PartiQLValueType
    ) : FunctionParameter()
}
