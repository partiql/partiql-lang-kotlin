package org.partiql.planner.catalog

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
    private val levels: Array<Identifier>,
) : Iterable<Identifier> {

    public fun getLevels(): Array<Identifier> {
        return levels
    }

    public fun getLength(): Int {
        return levels.size
    }

    public fun isEmpty(): Boolean {
        return levels.isEmpty()
    }

    public operator fun get(index: Int): Identifier {
        return levels[index]
    }

    override fun forEach(action: Consumer<in Identifier>?) {
        levels.toList().forEach(action)
    }

    override fun iterator(): Iterator<Identifier> {
        return levels.iterator()
    }

    override fun spliterator(): Spliterator<Identifier> {
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
        for (level in levels) {
            result = 31 * result + level.hashCode()
        }
        return result
    }

    /**
     * Return the SQL identifier representation of this namespace.
     */
    public override fun toString(): String {
        return levels.joinToString(".")
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
            if (ignoreCase && !lhs.matches(rhs)) {
                return false
            } else if (lhs != rhs) {
                return false
            }
        }
        return true
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
            return Namespace(levels.map { Identifier.delimited(it) }.toTypedArray()
            )
        }

        @JvmStatic
        public fun of(identifier: Identifier): Namespace {
            return Namespace(arrayOf(identifier))
        }

        @JvmStatic
        public fun of(identifiers: Collection<Identifier>): Namespace {
            return Namespace(identifiers.toTypedArray())
        }
    }
}
