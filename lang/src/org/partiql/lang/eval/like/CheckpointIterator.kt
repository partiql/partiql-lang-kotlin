package org.partiql.lang.eval.like

/**
 * Extends [Iterator<T>] with the ability to save the current position and restore it later,
 * thereby allowing a kind of infinite lookahead.
 */
internal interface CheckpointIterator<T> : Iterator<T> {

    /**
     * Saves the current position on an internal stack.
     *
     * Every invocation of this function should be paired with either a [restoreCheckpoint] or [discardCheckpoint].
     */
    fun saveCheckpoint()

    /**
     * Sets the current position to the last saved checkpoint and pops it off of the internal stack.
     *
     * Do not call this function without invoking [saveCheckpoint] first.
     */
    fun restoreCheckpoint()

    /**
     * Discards position currently on the top of the internal stack.
     *
     * Do not call this function without invoking [saveCheckpoint] first.
     */
    fun discardCheckpoint()
}


