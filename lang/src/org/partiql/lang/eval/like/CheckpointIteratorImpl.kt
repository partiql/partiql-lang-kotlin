package org.partiql.lang.eval.like

import java.util.Stack

interface CheckpointIterator<T> : Iterator<T> {
    fun skipToEnd()
    fun checkpoint()
    fun restore()
}

class CheckpointIteratorImpl<T>(private val backingList: List<T>) : CheckpointIterator<T> {
    private val checkpointStack = Stack<Int>()
    private var idx = -1

    override fun hasNext(): Boolean = (backingList.size - 1) > idx

    override fun next(): T {
        if(!hasNext()) throw NoSuchElementException()
        return backingList[++idx]
    }

    override fun skipToEnd() {
        idx = backingList.size - 1
    }

    override fun checkpoint() {
        checkpointStack.push(idx)
    }

    override fun restore() {
        idx = checkpointStack.pop()
    }
}


class CheckointCodepointIterator(private val str: String) : CheckpointIterator<Int> {
    private val checkpointStack = Stack<Int>()
    private val codepointCount = str.codePointCount(0, str.length)
    private var idx = -1

    override fun hasNext(): Boolean = (codepointCount - 1) > idx

    override fun next(): Int {
        if(!hasNext()) throw NoSuchElementException()
        return str.codePointAt(++idx)
    }

    override fun skipToEnd() {
        idx = codepointCount
    }

    override fun checkpoint() {
        checkpointStack.push(idx)
    }

    override fun restore() {
        idx = checkpointStack.pop()
    }


}
