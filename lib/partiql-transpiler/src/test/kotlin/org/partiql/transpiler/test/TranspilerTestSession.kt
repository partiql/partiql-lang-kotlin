package org.partiql.transpiler.test

public class TranspilerTestSession(
    public val catalog: String,
    public val path: List<String>,
    public val vars: Map<String, String>,
)