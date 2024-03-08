package org.partiql.types

public data class GraphType(
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "graph"
}
