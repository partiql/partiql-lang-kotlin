
package org.partiql.plugins.mockdb

import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.PartiQLValueType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.Int8Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.float64Value

@OptIn(PartiQLFunctionExperimental::class)
object Pow : PartiQLFunction {
    override val signature = FunctionSignature(
        name = "test_power",
        returns = PartiQLValueType.FLOAT64,
        parameters = listOf(
            FunctionParameter.ValueParameter(name = "base", type = PartiQLValueType.INT8),
            FunctionParameter.ValueParameter(name = "exponent", type = PartiQLValueType.INT8)
        ),
        isDeterministic = true,
        description = "Power [base] with [exponent]"
    )

    @OptIn(PartiQLValueExperimental::class)
    override operator fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue {
        val base = (arguments[0] as? Int8Value)?.int ?: 0
        val exponent = (arguments[1] as? Int8Value)?.int ?: 0
        val processed = Math.pow(base.toDouble(), exponent.toDouble())
        return float64Value(processed)
    }
}
