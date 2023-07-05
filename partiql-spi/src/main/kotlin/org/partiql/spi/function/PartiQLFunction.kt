package org.partiql.spi.function

import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.Parameter.ValueParameter
import org.partiql.types.PartiQLValueType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

public interface PartiQLFunction {
    public val signature: Signature
    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue
}

public class Signature(
    public val names: List<String>,
    public val returns: PartiQLValueType,
    public val parameters: List<Parameter> = emptyList(),
    public val isDeterministic: Boolean = true,
    public val description: String? = null
)

/**
 * This currently only contains [ValueParameter], however, this is designed in this
 * manner to allow for other types of parameters in the future. TypeParameter may be
 * of use.
 */
public sealed class Parameter {
    public class ValueParameter(
        public val name: String, // A human-readable name to help clarify use
        public val type: PartiQLValueType // The parameter's type
    ) : Parameter()
}
