package org.partiql.lang.eval

/**
 * Represents a namespaced global binding.
 * @property parts a collection of the parts of the identifier.
 */
data class BindingId(
    val parts: List<BindingName>
) : Iterable<BindingName> {

    init {
        assert(parts.any())
    }

    constructor(vararg names: BindingName) : this(names.toList())

    override fun iterator(): Iterator<BindingName> {
        return parts.iterator()
    }

    override fun toString(): String {
        return when (this.parts.size > 1) {
            true -> parts.joinToString(".")
            false -> parts[0].toString()
        }
    }
}
