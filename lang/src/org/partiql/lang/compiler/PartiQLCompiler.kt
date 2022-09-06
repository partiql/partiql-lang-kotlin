package org.partiql.lang.compiler

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.PartiQLStatement

/**
 * [PartiQLCompiler] is responsible for transforming a [PartiqlPhysical.Plan] into an executable [PartiQLStatement].
 */
interface PartiQLCompiler {

    /**
     * Compiles the [PartiqlPhysical.Plan] to an executable [PartiQLStatement].
     */
    fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement
}
