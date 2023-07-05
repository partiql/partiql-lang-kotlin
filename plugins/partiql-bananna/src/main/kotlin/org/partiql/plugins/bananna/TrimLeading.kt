
package org.partiql.plugins.bananna

import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.Signature
import org.partiql.types.PartiQLValueType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.stringValue

object TrimLeading : PartiQLFunction {
    override val signature = Signature(
        names = listOf("trim_leading"),
        returns = PartiQLValueType.STRING,
        parameters = listOf(
            Parameter.ValueParameter(name = "str", type = PartiQLValueType.STRING)
        ),
        isDeterministic = true,
        description = "Trims leading whitespace of a [str]."
    )

    @OptIn(PartiQLValueExperimental::class)
    override operator fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue {
        val str = (arguments[0] as? StringValue)?.string ?: ""
        val processed = str.trimStart()
        return stringValue(processed)
    }
}
