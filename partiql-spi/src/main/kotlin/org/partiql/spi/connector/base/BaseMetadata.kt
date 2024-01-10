package org.partiql.spi.connector.base

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.function.FunctionSignature

/**
 * [BaseMetadata] provides a plugin implementation with the default PartiQL INFORMATION_SCHEMA.
 */
public abstract class BaseMetadata: ConnectorMetadata {

    /**
     * Override routines
     */
    public open val routines: BaseRoutines = BaseRoutines()

    /**
     * This is analogous to the default INFORMATION_SCHEMA.ROUTINES base table.
     *
     * @param path
     * @return
     */
    override fun getScalarFunctions(path: BindingPath): List<ConnectorHandle<FunctionSignature.Scalar>> {
        TODO("Not yet implemented")
    }

    /**
     * This is analogous to the default INFORMATION_SCHEMA.ROUTINES base table but for aggregation functions.
     *
     * @param path
     * @return
     */
    override fun getAggregationFunctions(path: BindingPath): List<ConnectorHandle<FunctionSignature.Aggregation>> {
        TODO("Not yet implemented")
    }

    private companion object {

        const val INFORMATION_SCHEMA = "INFORMATION_SCHEMA"

    }
}
