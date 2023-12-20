package org.partiql.eval

import org.partiql.spi.connector.ConnectorBindings

class PartiQLEngineBuilder {

    private var catalogs: MutableMap<String, ConnectorBindings> = mutableMapOf()

    /**
     * Build the builder, return an implementation of a [PartiQLEngine]
     *
     * @return
     */
    public fun build(): PartiQLEngine = PartiQLEngineDefault(catalogs)

    /**
     * Java style method for assigning a Catalog name to [ConnectorBindings].
     *
     * @param catalog
     * @param metadata
     * @return
     */
    public fun addCatalog(catalog: String, bindings: ConnectorBindings): PartiQLEngineBuilder = this.apply {
        this.catalogs[catalog] = bindings
    }

    /**
     * Kotlin style method for assigning Catalog names to [ConnectorBindings].
     *
     * @param catalogs
     * @return
     */
    public fun catalogs(vararg catalogs: Pair<String, ConnectorBindings>): PartiQLEngineBuilder = this.apply {
        this.catalogs = mutableMapOf(*catalogs)
    }
}
