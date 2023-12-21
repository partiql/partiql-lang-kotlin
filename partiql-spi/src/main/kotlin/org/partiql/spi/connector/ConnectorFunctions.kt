package org.partiql.spi.connector

import org.partiql.types.function.FunctionSignature

/**
 * ConnectorFunctions holds the function signatures for a catalogs builtins.
 */
public abstract class ConnectorFunctions {

    /**
     * Scalar function signatures available via call syntax.
     */
    public open val functions: List<FunctionSignature.Scalar> = emptyList()

    /**
     * Scalar function signatures available via operator or special form syntax.
     */
    public open val operators: List<FunctionSignature.Scalar> = emptyList()

    /**
     * Aggregation function signatures.
     */
    public open val aggregations: List<FunctionSignature.Aggregation> = emptyList()
}
