package org.partiql.beam.io.runtime

public abstract class RidlList<T>(items: ArrayList<T>) : Collection<T> by items

public abstract class RidlArray<T>(private val items: Array<T>) {

    public val size: Int = items.size

    public operator fun get(index: Int): T = items[index]

    public operator fun set(index: Int, value: T): Unit = items.set(index, value)

    public operator fun iterator(): Iterator<T> = items.iterator()
}
