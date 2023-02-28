package org.partiql.spi.sources

import org.partiql.lang.types.StaticType

public class ColumnMetadata(
    public val name: String,
    public val type: StaticType,
    public val comment: String?,
    public val metas: Map<String, Any>
)
