package org.partiql.lang.compiler

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.PartiQLStatement

interface PartiQLCompiler {

    fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement
}
