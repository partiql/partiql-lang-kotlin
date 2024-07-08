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
    private val levels: Array<String>,
) : Iterable<String> {

    public fun getLevels(): Array<String> {
        return levels
    }

    public fun getLength(): Int {
        return levels.size
    }

    public fun isEmpty(): Boolean {
        return levels.isEmpty()
    }

    public operator fun get(index: Int): String {
        return levels[index]
    }

    override fun forEach(action: Consumer<in String>?) {
        levels.toList().forEach(action)
    }

    override fun iterator(): Iterator<String> {
        return levels.iterator()
    }

    override fun spliterator(): Spliterator<String> {
        return levels.toList().spliterator()
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val namespace = other as Namespace
        return levels.contentEquals(namespace.levels)
    }

    public override fun hashCode(): Int {
        return levels.contentHashCode()
    }

    public override fun toString(): String {
        return levels.joinToString(".")
    }

    public companion object {

        private val EMPTY = Namespace(emptyArray())

        public fun empty(): Namespace = EMPTY

        @JvmStatic
        public fun of(vararg levels: String): Namespace {
            if (levels.isEmpty()) {
                return empty()
            }
            return Namespace(arrayOf(*levels))
        }

        @JvmStatic
        public fun of(levels: Collection<String>): Namespace {
            if (levels.isEmpty()) {
                return empty()
            }
            return Namespace(levels.toTypedArray())
        }
    }
}
