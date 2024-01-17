package org.partiql.eval

import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

class PartiQLEngineBuilder {

    @OptIn(PartiQLFunctionExperimental::class)
    private var session: PartiQLEngine.Session = PartiQLEngine.Session(mutableMapOf(), mutableMapOf())

    /**
     * Build the builder, return an implementation of a [PartiQLEngine]
     *
     * @return
     */
    public fun build(): PartiQLEngine = PartiQLEngineDefault(session)

    /**
     * Java style method for assigning a Catalog name to [ConnectorBindings].
     *
     * @param catalog
     * @param bindings
     * @return
     */
    public fun addBinding(catalog: String, bindings: ConnectorBindings): PartiQLEngineBuilder = this.apply {
        this.session.bindings[catalog] = bindings
    }

    @OptIn(PartiQLFunctionExperimental::class)
    public fun addFunctions(catalog: String, functions: List<PartiQLFunction>): PartiQLEngineBuilder = this.apply {
        this.session.functions[catalog] = functions
    }

    /**
     * Kotlin style method for assigning Catalog names to [ConnectorBindings].
     *
     * @param catalogs
     * @return
     */
//    public fun catalogs(vararg catalogs: Pair<String, ConnectorBindings>): PartiQLEngineBuilder = this.apply {
//        this.catalogs = mutableMapOf(*catalogs)
//    }
}
