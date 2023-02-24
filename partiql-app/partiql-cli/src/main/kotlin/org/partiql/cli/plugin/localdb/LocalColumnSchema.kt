package org.partiql.cli.plugin.localdb

class LocalColumnSchema(
    public val name: String,
    public val type: String,
    public val typeParams: List<String>
)
