package org.partiql.plugins.local.functions

import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StringValue
import org.partiql.value.stringValue

@OptIn(PartiQLFunctionExperimental::class)
object TrimLead : PartiQLFunction {

    @OptIn(PartiQLValueExperimental::class)
    override val signature = FunctionSignature.Scalar(
        name = "trim_lead",
        returns = PartiQLValueType.STRING,
        parameters = listOf(
            FunctionParameter(name = "str", type = PartiQLValueType.STRING)
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
