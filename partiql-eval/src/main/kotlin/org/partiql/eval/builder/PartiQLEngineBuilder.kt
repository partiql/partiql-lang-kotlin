package org.partiql.eval.builder

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.SqlCompiler

class PartiQLEngineBuilder {

    /**
     * Build the builder, return an implementation of a [PartiQLEngine]
     *
     * @return
     */
    public fun build(): PartiQLEngine = SqlCompiler()
}
