package org.partiql.transpiler.targets

import org.partiql.transpiler.Dialect
import org.partiql.transpiler.TranspilerTarget
import org.partiql.transpiler.dialects.PartiQLDialect

/**
 * Default PartiQL Target
 */
public object PartiQLTarget : TranspilerTarget() {

    override val target: String = "PartiQL"

    override val version: String = "0.0"

    override val dialect: Dialect = PartiQLDialect.INSTANCE
}
