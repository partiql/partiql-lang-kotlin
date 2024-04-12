package org.partiql.eval.internal.helpers

/**
 * WARNING: You must invoke [hasNext] before calling [next].
 */
internal class IteratorChain<T>(
    iterators: Iterable<Iterator<T>>
) : Iterator<T> {

    private var iterator = iterators.iterator()
    private var current = iterator.next()

    override fun hasNext(): Boolean {
        return when (current.hasNext()) {
            true -> true
            false -> {
                if (!iterator.hasNext()) {
                    return false
                }
                current = iterator.next()
                current.hasNext()
            }
        }
    }

    override fun next(): T {
        return current.next()
    }
}
