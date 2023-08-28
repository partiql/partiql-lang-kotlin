package org.partiql.transpiler.test.targets

import org.partiql.transpiler.test.TranspilerTest
import org.partiql.transpiler.test.TranspilerTestTarget

/**
 * Sanity end-to-end PartiQL test evaluator.
 */
class PartiQLTestTarget(suite: String) : TranspilerTestTarget("partiql", suite) {

    override fun assert(test: TranspilerTest) {
        TODO("Not yet implemented")
    }
}
