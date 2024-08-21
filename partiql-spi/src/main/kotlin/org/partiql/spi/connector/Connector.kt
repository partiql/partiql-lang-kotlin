package org.partiql.spi.connector

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.emptyIonStruct
import org.partiql.planner.catalog.Catalog

/**
 * A mechanism by which PartiQL can access bindings and catalog metadata.
 */
public interface Connector {

    /**
     * Returns a [ConnectorBindings] which the engine uses to load values.
     */
    public fun getBindings(): ConnectorBindings

    /**
     * Returns a [Catalog] which the planner uses to load catalog metadata.
     */
    public fun getCatalog(): Catalog

    /**
     * A Plugin leverages a [Factory] to produce a [Connector] which is used for binding and metadata access.
     */
    public interface Factory {

        /**
         * The connector name used to register the factory.
         */
        public val name: String

        /**
         * The connector factory method.
         *
         * @param config
         * @return
         */
        public fun create(config: StructElement = emptyIonStruct()): Connector
    }
}
