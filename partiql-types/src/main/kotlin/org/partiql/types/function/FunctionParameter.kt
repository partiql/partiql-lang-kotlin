package org.partiql.types.function

import org.partiql.types.PartiQLValueType

/**
 * This currently only contains [ValueParameter], however, this is designed in this
 * manner to allow for other types of parameters in the future. TypeParameter may be
 * of use.
 */
public sealed class FunctionParameter {
    public class ValueParameter(
        public val name: String, // A human-readable name to help clarify use
        public val type: PartiQLValueType // The parameter's type
    ) : FunctionParameter()
}
