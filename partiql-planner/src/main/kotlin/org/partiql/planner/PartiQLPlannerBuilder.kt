package org.partiql.planner

import org.partiql.spi.connector.ConnectorMetadata

/**
 * PartiQLPlannerBuilder is used to programmatically construct a [PartiQLPlanner] implementation.
 *
 * Usage:
 *      PartiQLPlanner.builder()
 *                    .addCatalog("foo", FooConnector())
 *                    .addCatalog("bar", BarConnector())
 *                    .builder()
 */
public class PartiQLPlannerBuilder {

    private var headers: MutableList<Header> = mutableListOf(PartiQLHeader)
    private var catalogs: MutableMap<String, ConnectorMetadata> = mutableMapOf()
    private var passes: List<PartiQLPlannerPass> = emptyList()

    /**
     * Build the builder, return an implementation of a [PartiQLPlanner].
     *
     * @return
     */
    public fun build(): PartiQLPlanner = PartiQLPlannerDefault(headers, catalogs, passes)

    /**
     * Java style method for assigning a Catalog name to [ConnectorMetadata].
     *
     * @param catalog
     * @param metadata
     * @return
     */
    public fun addCatalog(catalog: String, metadata: ConnectorMetadata): PartiQLPlannerBuilder = this.apply {
        this.catalogs[catalog] = metadata
    }

    /**
     * Kotlin style method for assigning Catalog names to [ConnectorMetadata].
     *
     * @param catalogs
     * @return
     */
    public fun catalogs(vararg catalogs: Pair<String, ConnectorMetadata>): PartiQLPlannerBuilder = this.apply {
        this.catalogs = mutableMapOf(*catalogs)
    }

    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes = passes
    }
}
