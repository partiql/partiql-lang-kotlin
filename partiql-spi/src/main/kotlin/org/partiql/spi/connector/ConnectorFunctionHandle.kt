package org.partiql.spi.connector

import org.partiql.types.function.FunctionSignature

/**
 * A [ConnectorFunctionHandle] represents the location and typing information for a [ConnectorFunction].
 */
public sealed interface ConnectorFunctionHandle {

    /**
     * The absolute path to this function's definition in the catalog.
     */
    public val path: ConnectorPath

    /**
     * The function's type definition.
     */
    public val signature: FunctionSignature

    /**
     * Handle to a scalar function.
     *
     * @property path
     * @property signature
     */
    public data class Scalar(
        override val path: ConnectorPath,
        override val signature: FunctionSignature.Scalar,
    ) : ConnectorFunctionHandle

    /**
     * Handle to an aggregation function.
     *
     * @property path
     * @property signature
     */
    public data class Aggregation(
        override val path: ConnectorPath,
        override val signature: FunctionSignature.Aggregation,
    ) : ConnectorFunctionHandle
}
