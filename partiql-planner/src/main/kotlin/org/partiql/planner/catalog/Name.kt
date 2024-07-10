package org.partiql.planner.catalog

import org.partiql.ast.Identifier
import org.partiql.ast.sql.sql

/**
 * A reference to a named object in a catalog.
 */
public class Name(
    private val namespace: Namespace,
    private val name: Identifier.Symbol,
) {

    public fun getNamespace(): Namespace = namespace

    public fun hasNamespace(): Boolean = !namespace.isEmpty()

    public fun getName(): String = name.symbol

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
        result = 31 * result + name.symbol.hashCode()
        return result
    }

    /**
     * Return the SQL identifier representation of this name.
     */
    override fun toString(): String {
        return if (namespace.isEmpty()) {
            name.sql()
        } else {
            "${namespace}.${name.sql()}"
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
        if (ignoreCase && !matches(name, other.name)) {
            return false
        } else if (name.symbol != other.name.symbol) {
            return false
        }
        return this.namespace.matches(other.namespace, ignoreCase)
    }

    // TODO de-duplicate or define on Identifier class
    private fun matches(lhs: Identifier.Symbol, rhs: Identifier.Symbol): Boolean {
        val ignoreCase = (
            lhs.caseSensitivity == Identifier.CaseSensitivity.INSENSITIVE ||
                rhs.caseSensitivity == Identifier.CaseSensitivity.INSENSITIVE
            )
        return lhs.symbol.equals(rhs.symbol, ignoreCase)
    }

    public companion object {

        @JvmStatic
        public fun of(vararg names: String): Name = of(names.toList())

        @JvmStatic
        public fun of(names: Collection<String>): Name {
            assert(names.size > 1) { "Cannot create an empty name" }
            val namespace = Namespace.of(names.drop(1))
            val name = Identifier.Symbol(names.last(), Identifier.CaseSensitivity.SENSITIVE)
            return Name(namespace, name)
        }

        @JvmStatic
        public fun of(identifier: Identifier): Name = when (identifier) {
            is Identifier.Qualified -> of(identifier)
            is Identifier.Symbol -> of(identifier)
        }

        @JvmStatic
        public fun of(identifier: Identifier.Symbol): Name {
            return Name(
                namespace = Namespace.root(),
                name = identifier
            )
        }

        @JvmStatic
        public fun of(identifier: Identifier.Qualified): Name {
            val identifiers = mutableListOf<Identifier.Symbol>()
            identifiers.add(identifier.root)
            identifiers.addAll(identifier.steps)
            return of(identifiers)
        }

        @JvmStatic
        public fun of(identifiers: Collection<Identifier.Symbol>): Name {
            assert(identifiers.size > 1) { "Cannot create an empty name" }
            val namespace = Namespace.of(identifiers.drop(1))
            val name = identifiers.last()
            return Name(namespace, name)
        }
    }
}
