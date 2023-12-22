package org.partiql.plugin

import org.partiql.spi.connector.ConnectorFunctions
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionSignature

object PartiQLFunctions : ConnectorFunctions() {

    @OptIn(PartiQLFunctionExperimental::class)
    override val functions = PartiQLPlugin.scalars.map { it.signature }

    @OptIn(PartiQLFunctionExperimental::class)
    override val operators: List<FunctionSignature.Scalar> = PartiQLPlugin.operators.map { it.signature }

    @OptIn(PartiQLFunctionExperimental::class)
    override val aggregations = PartiQLPlugin.aggregations.map { it.signature }
}
