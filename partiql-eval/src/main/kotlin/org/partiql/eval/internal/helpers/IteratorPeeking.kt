package org.partiql.eval.internal.helpers

/**
 * For [Iterator]s that MUST materialize data in order to execute [hasNext], this abstract class caches the
 * result of [peek] to implement both [hasNext] and [next].
 *
 * With this implementation, invoking hasNext() multiple times will not iterate unnecessarily. Invoking next() without
 * invoking hasNext() is allowed -- however, it is highly recommended to avoid doing so.
 */
internal abstract class IteratorPeeking<T> : Iterator<T> {

    internal var next: T? = null

    /**
     * @return NULL when there is not another [T] to be produced. Returns a [T] when able to.
     *
     * @see IteratorPeeking
     */
    abstract fun peek(): T?

    override fun hasNext(): Boolean {
        if (next != null) {
            return true
        }
        this.next = peek()
        return this.next != null
    }

    override fun next(): T {
        val next = next
            ?: peek()
            ?: error("There were no more elements, however, next() was called. Please use hasNext() beforehand.")
        this.next = null
        return next
    }
}
