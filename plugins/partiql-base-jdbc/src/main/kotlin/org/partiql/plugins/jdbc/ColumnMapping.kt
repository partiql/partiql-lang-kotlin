package org.partiql.plugins.jdbc

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

@OptIn(PartiQLValueExperimental::class)
public class ColumnMapping(
    public val partiQLValueType: PartiQLValueType,
    public val readFunction: ReadFunction
)
