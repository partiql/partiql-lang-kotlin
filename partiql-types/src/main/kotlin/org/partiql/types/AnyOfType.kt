package org.partiql.types

/**
 * Represents a [StaticType] that's defined by the union of multiple [StaticType]s.
 */
public data class AnyOfType(val types: Set<StaticType>, override val metas: Map<String, Any> = mapOf()) : StaticType {
    /**
     * Flattens a union type by traversing the types and recursively bubbling up the underlying union types.
     *
     * If union type ends up having just one type in it, then that type is returned.
     */
    override fun flatten(): StaticType = this.copy(
        types = this.types.flatMap {
            when (it) {
                is SingleType -> listOf(it)
                is AnyType -> listOf(it)
                is AnyOfType -> it.types
            }
        }.toSet()
    ).let {
        when {
            it.types.size == 1 -> it.types.first()
            it.types.filterIsInstance<AnyOfType>().any() -> it.flatten()
            else -> it
        }
    }

    override fun toString(): String =
        when (val flattenedType = flatten()) {
            is AnyOfType -> {
                val unionedTypes = flattenedType.types
                when (unionedTypes.size) {
                    0 -> "\$null"
                    1 -> unionedTypes.first().toString()
                    else -> {
                        val types = unionedTypes.joinToString { it.toString() }
                        "union($types)"
                    }
                }
            }
            else -> flattenedType.toString()
        }

    override val allTypes: List<StaticType>
        get() = this.types.map { it.flatten() }
}
