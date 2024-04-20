package org.partiql.plugins.jdbc

public class JdbcTable(
    public val catalog: String?,
    public val schema: String?,
    public val columns: List<JdbcColumn>
)
