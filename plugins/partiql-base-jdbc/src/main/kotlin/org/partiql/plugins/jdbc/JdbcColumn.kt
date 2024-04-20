package org.partiql.plugins.jdbc

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

public class JdbcColumn @OptIn(PartiQLValueExperimental::class) constructor(
    public val columnName: String,
    public val columnJdbcType: JdbcType,
    public val partiQLValueType: PartiQLValueType,
    public val nullable: Boolean
)
