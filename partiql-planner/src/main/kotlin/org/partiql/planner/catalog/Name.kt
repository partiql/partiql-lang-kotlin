package org.partiql.planner.catalog

/**
 * A reference to a named object in a catalog.
 */
public class Name(
    private val namespace: Namespace,
    private val name: Identifier,
) {

    public fun getNamespace(): Namespace = namespace

    public fun hasNamespace(): Boolean = !namespace.isEmpty()

    public fun getName(): Identifier = name

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
        return matches(other as Name, ignoreCase = false)
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
     * Return the SQL name representation of this name.
     */
    override fun toString(): String {
        return if (namespace.isEmpty()) {
            name.toString()
        } else {
            "$namespace.$name"
        }
    }

    /**
     * Compares one name to another, possibly ignoring case.
     *
     * @param other         The other name to match against.
     * @param ignoreCase    If false, the compare all levels case-sensitively (exact-case match).
     * @return
     */
    public fun matches(other: Name, ignoreCase: Boolean = false): Boolean {
        if (ignoreCase && !(this.name.matches(other.name))) {
            return false
        } else if (name != other.name) {
            return false
        }
        return this.namespace.matches(other.namespace, ignoreCase)
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
            assert(names.size > 1) { "Cannot create an empty name" }
            val namespace = Namespace.of(names.drop(1))
            val name = Identifier.delimited(names.last())
            return Name(namespace, name)
        }

        @JvmStatic
        public fun of(name: Identifier): Name {
            return Name(
                namespace = Namespace.root(),
                name = name
            )
        }

        @JvmStatic
        public fun of(names: Collection<Identifier>): Name {
            assert(names.size > 1) { "Cannot create an empty name" }
            val namespace = Namespace.of(names.drop(1))
            val name = names.last()
            return Name(namespace, name)
        }
    }
}
