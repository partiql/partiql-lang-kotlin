package org.partiql.spi.function

import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.PartiQLFunction.Parameter.ValueParameter
import org.partiql.types.PartiQLValueType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents a function interface that can be overridden by external teams.
 *
 * An implementation of this interface defines the behavior of the function
 * and its signature, which includes the function's names, return type, parameters,
 * determinism, and an optional description.
 */
@PartiQLFunctionExperimental
public interface PartiQLFunction {
    public val signature: Signature
    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue

    /**
     * Represents the signature of a PartiQL function.
     *
     * The signature includes the names of the function (which allows for function overloading),
     * the return type, a list of parameters, a flag indicating whether the function is deterministic
     * (i.e., always produces the same output given the same input), and an optional description.
     */
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
}
