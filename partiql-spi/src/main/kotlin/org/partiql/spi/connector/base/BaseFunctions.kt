package org.partiql.spi.connector.base

/* ktlint-disable no-wildcard-imports */
import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.spi.connector.ConnectorFunctions
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.types.function.FunctionSignature

/**
 * This class serves as an extendable
 */
@OptIn(ConnectorFunctionExperimental::class)
public open class BaseFunctions : ConnectorFunctions {

    public open val functions: Map<String, ConnectorFunction.Scalar> =
        BaseRoutines.scalar.associateBy { it.signature.specific }

    public open val aggregations: Map<String, ConnectorFunction.Aggregation> =
        BaseRoutines.aggregations.associateBy { it.signature.specific }

    override fun getScalarFunction(handle: ConnectorHandle<FunctionSignature.Scalar>): ConnectorFunction.Scalar? {
        return functions[handle.entity.specific]
    }

    override fun getAggregationFunction(handle: ConnectorHandle<FunctionSignature.Aggregation>): ConnectorFunction.Aggregation? {
        return aggregations[handle.entity.specific]
    }
}
