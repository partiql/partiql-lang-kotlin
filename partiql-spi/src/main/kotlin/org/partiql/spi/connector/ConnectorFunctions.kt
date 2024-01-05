package org.partiql.spi.connector

/**
 * A [ConnectorFunctions] implementation is responsible for linking a handle to a function implementation for execution.
 */
@ConnectorFunctionExperimental
public interface ConnectorFunctions {

    public fun getScalarFunction(handle: ConnectorFunctionHandle.Scalar): ConnectorFunction.Scalar

    public fun getAggregationFunction(handle: ConnectorFunctionHandle.Aggregation): ConnectorFunction.Aggregation
}
