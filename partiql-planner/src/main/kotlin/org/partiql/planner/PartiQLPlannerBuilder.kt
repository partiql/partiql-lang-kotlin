package org.partiql.planner

import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.function.FunctionSignature

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

    private var fns: MutableList<FunctionSignature.Scalar> = mutableListOf()
    private var catalogs: MutableMap<String, ConnectorMetadata> = mutableMapOf()
    private var passes: List<PartiQLPlannerPass> = emptyList()

    /**
     * Build the builder, return an implementation of a [PartiQLPlanner].
     *
     * @return
     */
    public fun build(): PartiQLPlanner {
        val headers = mutableListOf<Header>(PartiQLHeader)
        if (fns.isNotEmpty()) {
            val h = object : Header() {
                override val namespace: String = "UDF"
                override val functions = fns
            }
            headers.add(h)
        }
        return PartiQLPlannerDefault(headers, catalogs, passes)
    }

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

    /**
     * Java style method for adding a user-defined-function.
     *
     * @param function
     * @return
     */
    public fun addFunction(function: FunctionSignature.Scalar): PartiQLPlannerBuilder = this.apply {
        this.fns.add(function)
    }

    /**
     * Kotlin style method for adding a user-defined-function.
     *
     * @param function
     * @return
     */
    public fun functions(vararg functions: FunctionSignature.Scalar): PartiQLPlannerBuilder = this.apply {
        this.fns = mutableListOf(*functions)
    }

    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes = passes
    }
}
