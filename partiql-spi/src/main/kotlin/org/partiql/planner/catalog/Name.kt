package org.partiql.planner.catalog

import java.util.Spliterator
import java.util.function.Consumer

/**
 * A reference to a named object in a catalog.
 */
public class Name(
    private val namespace: Namespace,
    private val name: String,
) : Iterable<String> {

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
     * Returns an iterator of strings for this name.
     */
    private fun getParts(): List<String> {
        val parts = mutableListOf<String>()
        parts.addAll(namespace)
        parts.add(name)
        return parts
    }

    override fun forEach(action: Consumer<in String>?) {
        getParts().forEach(action)
    }

    override fun iterator(): Iterator<String> {
        return getParts().iterator()
    }

    override fun spliterator(): Spliterator<String> {
        return getParts().spliterator()
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
        return Identifier.delimited(getParts()).toString()
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
            val namespace = Namespace.of(names.take(names.size - 1))
            val name = names.last()
            return Name(namespace, name)
        }
    }
}
