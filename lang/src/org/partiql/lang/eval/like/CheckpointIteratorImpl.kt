package org.partiql.lang.eval.like

import java.util.Stack


/** An implementation of [CheckpointIterator] which is backed by a [List]. */
internal class CheckpointIteratorImpl<T>(private val backingList: List<T>) : CheckpointIterator<T> {
    private val checkpointStack = Stack<Int>()
    private var idx = -1

    override fun hasNext(): Boolean = (backingList.size - 1) > idx

    override fun next(): T {
        if(!hasNext()) throw NoSuchElementException()
        return backingList[++idx]
    }

    override fun saveCheckpoint() {
        checkpointStack.push(idx)
    }

    override fun restoreCheckpoint() {
        idx = checkpointStack.pop()
    }

    override fun discardCheckpoint() {
        checkpointStack.pop()
    }
}


