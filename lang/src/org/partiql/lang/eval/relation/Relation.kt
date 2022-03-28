package org.partiql.lang.eval.relation

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

/**
 * Builds a [RelationIterator] that yields after every step in evaluating a relational operator.
 *
 * This is inspired heavily by Kotlin's [sequence] but for [RelationIterator] instead of [Sequence].
 */
internal fun relation(
    seqType: RelationType,
    block: suspend RelationScope.() -> Unit
): RelationIterator {
    val iterator = RelationBuilderIterator(seqType, block)
    iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
    return iterator
}

/** Defines functions within a block supplied to [relation]. */
internal interface RelationScope {
    /** Suspends the coroutine.  Should be called after processing the current row of the relation. */
    suspend fun yield()

    /** Yields once for every row remaining in [relItr]. */
    suspend fun yieldAll(relItr: RelationIterator)
}

private class RelationBuilderIterator(
    override val relType: RelationType,
    block: suspend RelationScope.() -> Unit
) : RelationScope, RelationIterator, Continuation<Unit> {
    var yielded = false

    var nextStep: Continuation<Unit>? = block.createCoroutine(receiver = this, completion = this)

    override suspend fun yield() {
        yielded = true
        suspendCoroutineUninterceptedOrReturn<Unit> { c ->
            nextStep = c;
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun yieldAll(relItr: RelationIterator) {
        while(relItr.nextRow()) {
            yield()
        }
    }

    override fun nextRow(): Boolean {
        // if nextStep is null it means we've reached the end of the relation, but nextRow() was called again
        // for some reason.  This probably indicates a bug since we should not in general be attempting to
        // read a `RelationIterator` after it has exhausted.
        if(nextStep == null) {
            error("Relation was previously exhausted.  " +
                "Please don't call nextRow() again after it returns false the first time.")
        }
        val step = nextStep!!
        nextStep = null
        step.resume(Unit)

        return if(yielded) {
            yielded = false
            true
        } else {
            false
        }
    }

    // Completion continuation implementation
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it is there
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext


}