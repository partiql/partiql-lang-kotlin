package org.partiql.planner.catalog

import java.util.Spliterator
import java.util.function.Consumer

/**
 * The routine resolution path, accessible via PATH.
 */
public class Path private constructor(private val namespaces: List<Namespace>) : Iterable<Namespace> {

    public companion object {

        @JvmStatic
        public fun of(vararg namespaces: Namespace): Path = Path(namespaces.toList())
    }

    public fun getLength(): Int {
        return namespaces.size
    }

    public fun isEmpty(): Boolean {
        return namespaces.isEmpty()
    }

    public operator fun get(index: Int): Namespace {
        return namespaces[index]
    }

    override fun forEach(action: Consumer<in Namespace>?) {
        namespaces.forEach(action)
    }

    override fun iterator(): Iterator<Namespace> {
        return namespaces.iterator()
    }

    override fun spliterator(): Spliterator<Namespace> {
        return namespaces.spliterator()
    }

    override fun toString(): String = "PATH = (${namespaces.joinToString()})"
}
