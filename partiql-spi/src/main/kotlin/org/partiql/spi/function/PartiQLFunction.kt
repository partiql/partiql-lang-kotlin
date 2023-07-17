package org.partiql.spi.function

import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.PartiQLValueType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * TODO, this is pulled from https://github.com/partiql/partiql-lang-kotlin/blob/functionManager/partiql-spi/src/main/kotlin/org/partiql/spi/function/PartiQLFunction.kt
 */
public interface PartiQLFunction {

    public val signature: Signature

    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue

    public class Signature(
        public val symbol: String,
        public val returns: PartiQLValueType,
        public val parameters: List<Parameter> = emptyList(),
        public val isDeterministic: Boolean = true,
        public val description: String? = null
    )

    public sealed class Parameter {

        public class ValueParameter(
            public val name: String,
            public val type: PartiQLValueType
        ) : Parameter()

        public class TypeParameter(
            public val name: String,
            public val type: PartiQLValueType
        ) : Parameter()
    }
}
