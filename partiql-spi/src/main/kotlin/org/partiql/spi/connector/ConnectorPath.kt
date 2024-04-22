package org.partiql.spi.connector

import java.util.Spliterator
import java.util.function.Consumer

/**
 * A list of string that is used to uniquely identifier an entity in external resources.
 *
 * Implementation of the connector interface needs to convert [org.partiql.spi.BindingPath] to ConnectorPath.
 *
 * TODO: Have an API to implement the conversion logic, or consolidate binding path and conversion path.
 */
public data class ConnectorPath(public val steps: List<String>) : Iterable<String> {

    public companion object {

        @JvmStatic
        public fun of(vararg steps: String): ConnectorPath = ConnectorPath(steps.toList())
    }

    public operator fun get(index: Int): String = steps[index]

    override fun forEach(action: Consumer<in String>?): Unit = steps.forEach(action)

    override fun iterator(): Iterator<String> = steps.iterator()

    override fun spliterator(): Spliterator<String> = steps.spliterator()
}
