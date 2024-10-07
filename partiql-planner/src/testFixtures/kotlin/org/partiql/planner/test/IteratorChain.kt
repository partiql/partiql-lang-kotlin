package org.partiql.planner.test

internal class IteratorChain<T>(
    iterators: Iterable<Iterator<T>>
) : Iterator<T> {

    private var _iter: Iterator<Iterator<T>> = iterators.iterator()
    private var _currentIter: Iterator<T> = emptyList<T>().iterator()

    override fun hasNext(): Boolean {
        if (!_currentIter.hasNext()) {
            if (!_iter.hasNext()) {
                return false
            }
            _currentIter = _iter.next()
            return hasNext()
        }
        return true
    }

    override fun next(): T {
        return _currentIter.next()
    }
}
