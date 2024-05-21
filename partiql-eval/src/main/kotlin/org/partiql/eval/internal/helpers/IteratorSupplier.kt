package org.partiql.eval.internal.helpers

internal class IteratorSupplier<T>(private val supplier: () -> Iterator<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> {
        return supplier.invoke()
    }
}
