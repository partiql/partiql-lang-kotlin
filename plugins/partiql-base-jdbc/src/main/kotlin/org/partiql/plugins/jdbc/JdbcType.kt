package org.partiql.plugins.jdbc

public data class JdbcType(
    val jdbcType: Int, // JDBC Type code
    val jdbcTypeName: String,
    val precision: Int? = null,
    val scale: Int? = null,
    val columnSize: Int? = null
)
