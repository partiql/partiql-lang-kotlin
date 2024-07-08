package org.partiql.planner.catalog

/**
 * Thin wrapper over a list of strings.
 */
public data class Name(
    private val namespace: Namespace,
    private val name: String,
) {

    public fun getNamespace(): Namespace = namespace

    public fun hasNamespace(): Boolean = !namespace.isEmpty()

    public fun getName(): String = name

    public companion object {

        @JvmStatic
        public fun of(vararg names: String): Name {
            assert(names.size > 1) { "Cannot create an empty" }
            return Name(
                namespace = Namespace.of(*names.drop(1).toTypedArray()),
                name = names.last(),
            )
        }
    }
}
