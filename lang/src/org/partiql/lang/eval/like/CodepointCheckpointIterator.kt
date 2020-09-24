package org.partiql.lang.eval.like

import java.util.Stack

/** An implementation of [CheckpointIterator] that iterates over the unicode codepoints within a string. */
internal class CodepointCheckpointIterator(private val str: String) : CheckpointIterator<Int> {
    private val checkpointStack = Stack<Int>()
    private val codepointCount = str.codePointCount(0, str.length)
    private var idx = -1

    override fun hasNext(): Boolean = (codepointCount - 1) > idx

    override fun next(): Int {
        if(!hasNext()) throw NoSuchElementException()
        return str.codePointAt(++idx)
    }

    fun skipToEnd() {
        idx = codepointCount
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