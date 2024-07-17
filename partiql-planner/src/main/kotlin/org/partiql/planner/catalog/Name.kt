package org.partiql.planner.catalog

/**
 * A reference to a named object in a catalog.
 */
public class Name(
    private val namespace: Namespace,
    private val name: String,
) {

    /**
     * Returns the unqualified name part.
     */
    public fun getName(): String = name

    /**
     * Returns the name's namespace.
     */
    public fun getNamespace(): Namespace = namespace

    /**
     * Returns true if the namespace is non-empty.
     */
    public fun hasNamespace(): Boolean = !namespace.isEmpty()

    /**
     * Returns a list of strings representing the path of this name.
     */
    public fun getPath(): List<String> {
        val parts = mutableListOf<String>()
        parts.addAll(namespace.getLevels())
        parts.add(name)
        return parts
    }

    /**
     * Compares two names including their namespaces and symbols.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        other as Name
        return (this.name == other.name) && (this.namespace == other.namespace)
    }

    /**
     * The hashCode() is case-sensitive.
     */
    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + namespace.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    /**
     * Return the SQL name representation of this name — all parts delimited.
     */
    override fun toString(): String {
        return Identifier.delimited(getPath()).toString()
    }

    public companion object {

        /**
         * Construct a name from a string.
         */
        @JvmStatic
        public fun of(vararg names: String): Name = of(names.toList())

        /**
         * Construct a name from a collection of strings.
         */
        @JvmStatic
        public fun of(names: Collection<String>): Name {
            assert(names.size > 0) { "Cannot create an empty name" }
            val namespace = Namespace.of(names.drop(1))
            val name = names.last()
            return Name(namespace, name)
        }
    }
}
