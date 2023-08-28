package org.partiql.transpiler.test

import org.partiql.types.StaticType

/**
 * TODO replace or improve [StaticType].
 */
public class TranspilerTest(
    public val statement: String,
    public val schema: StaticType,
)
