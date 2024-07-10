package org.partiql.planner.catalog

import org.partiql.ast.Identifier
import org.partiql.ast.sql.sql
import java.util.Spliterator
import java.util.function.Consumer

/**
 * A reference to a namespace within a catalog.
 *
 * Related
 *  - Iceberg — https://github.com/apache/iceberg/blob/main/api/src/main/java/org/apache/iceberg/catalog/Namespace.java
 *  - Calcite — https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/schema/Schema.java
 */
public class Namespace private constructor(
    private val levels: Array<Identifier.Symbol>,
) : Iterable<Identifier.Symbol> {

    public fun getLevels(): Array<Identifier.Symbol> {
        return levels
    }

    public fun getLength(): Int {
        return levels.size
    }

    public fun isEmpty(): Boolean {
        return levels.isEmpty()
    }

    public operator fun get(index: Int): Identifier.Symbol {
        return levels[index]
    }

    override fun forEach(action: Consumer<in Identifier.Symbol>?) {
        levels.toList().forEach(action)
    }

    override fun iterator(): Iterator<Identifier.Symbol> {
        return levels.iterator()
    }

    override fun spliterator(): Spliterator<Identifier.Symbol> {
        return levels.toList().spliterator()
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return matches(other as Namespace, ignoreCase = false)
    }

    /**
     * The hashCode() is case-sensitive — java.util.Arrays.hashCode
     */
    public override fun hashCode(): Int {
        var result = 1
        for (element in levels) result = 31 * result + element.symbol.hashCode()
        return result
    }

    /**
     * Return the SQL identifier representation of this namespace.
     */
    public override fun toString(): String {
        return levels.joinToString(".") { it.sql() }
    }

    /**
     * Compares one namespace to another, possibly ignoring case.
     *
     * Note that ignoring case pushes the case-sensitivity check down to the identifier matching.
     *
     * For example,
     *    "x" and "X" are NOT equal regardless of ignoreCase because they are delimited.
     *     x  and  X  are NOT equal with ignoreCase=false but are matching identifiers.
     *
     * @param other         The other namespace to match against.
     * @param ignoreCase    If false, then compare all levels case-sensitively (exact-case match).
     * @return
     */
    public fun matches(other: Namespace, ignoreCase: Boolean = false): Boolean {
        val n = getLength()
        if (n != other.getLength()) {
            return false
        }
        for (i in 0 until n) {
            val lhs = levels[i]
            val rhs = other[i]
            if (ignoreCase && !matches(lhs, rhs)) {
                return false
            } else if (lhs.symbol != rhs.symbol) {
                return false
            }
        }
        return true
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

        private val ROOT = Namespace(emptyArray())

        public fun root(): Namespace = ROOT

        @JvmStatic
        public fun of(vararg levels: String): Namespace = of(levels.toList())

        @JvmStatic
        public fun of(levels: Collection<String>): Namespace {
            if (levels.isEmpty()) {
                return root()
            }
            return Namespace(
                levels
                    .map { Identifier.Symbol(it, Identifier.CaseSensitivity.SENSITIVE) }
                    .toTypedArray()
            )
        }

        @JvmStatic
        public fun of(identifier: Identifier): Namespace = when (identifier) {
            is Identifier.Qualified -> of(identifier)
            is Identifier.Symbol -> of(identifier)
        }

        @JvmStatic
        public fun of(identifier: Identifier.Symbol): Namespace {
            return Namespace(arrayOf(identifier))
        }

        @JvmStatic
        public fun of(identifier: Identifier.Qualified): Namespace {
            val levels = mutableListOf<Identifier.Symbol>()
            levels.add(identifier.root)
            levels.addAll(identifier.steps)
            return Namespace(levels.toTypedArray())
        }

        @JvmStatic
        public fun of(identifiers: Collection<Identifier.Symbol>): Namespace {
            return Namespace(identifiers.toTypedArray())
        }
    }
}
