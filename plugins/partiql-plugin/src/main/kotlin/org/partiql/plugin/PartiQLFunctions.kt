package org.partiql.plugin

import org.partiql.spi.connector.ConnectorFunctions
import org.partiql.spi.function.PartiQLFunctionExperimental

@OptIn(PartiQLFunctionExperimental::class)
object PartiQLFunctions : ConnectorFunctions() {

    override val functions = PartiQLPlugin.scalars.map { it.signature }

    override val operators = PartiQLPlugin.operators.map { it.signature }

    override val aggregations = PartiQLPlugin.aggregations.map { it.signature }
}
