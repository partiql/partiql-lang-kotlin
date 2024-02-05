package org.partiql.eval

class PartiQLEngineBuilder {

    /**
     * Build the builder, return an implementation of a [PartiQLEngine]
     *
     * @return
     */
    public fun build(): PartiQLEngine = PartiQLEngineDefault()
}
