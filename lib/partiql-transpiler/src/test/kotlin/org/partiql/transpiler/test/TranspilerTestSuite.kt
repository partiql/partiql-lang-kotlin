package org.partiql.transpiler.test

public class TranspilerTestSuite(
    public val name: String,
    public val session: TranspilerTestSession,
    public val tests: Map<String, TranspilerTest>,
)