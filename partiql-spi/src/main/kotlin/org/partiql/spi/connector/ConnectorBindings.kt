package org.partiql.spi.connector

import org.partiql.planner.catalog.Name

/**
 * TODO REMOVE ME IN FAVOR OF SCANNING FROM A CATALOG IMPLEMENTATION
 *
 * Top-level interface for loading data into the engine.
 */
public interface ConnectorBindings {

    /**
     * Get a binding for the given name (or null).
     */
    public fun getBinding(name: Name): ConnectorBinding?
}
