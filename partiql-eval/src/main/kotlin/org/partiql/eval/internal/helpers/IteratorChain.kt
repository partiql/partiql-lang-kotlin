package org.partiql.eval.internal.helpers

internal class IteratorChain<T>(
    iterators: Array<Iterator<T>>
) : IteratorPeeking<T>() {

    private var iterator: Iterator<Iterator<T>> = when (iterators.isEmpty()) {
        true -> listOf(emptyList<T>().iterator()).iterator()
        false -> iterators.iterator()
    }
    private var current: Iterator<T> = iterator.next()

    override fun peek(): T? {
        return when (current.hasNext()) {
            true -> current.next()
            false -> {
                while (iterator.hasNext()) {
                    current = iterator.next()
                    if (current.hasNext()) {
                        return current.next()
                    }
                }
                return null
            }
        }
    }
}
