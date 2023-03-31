package org.partiql.lang.eval.physical

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ThunkFactory
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.fillErrorContext
import org.partiql.lang.eval.relation.RelationIterator

/** A thunk that returns a [RelationIterator], which is the result of evaluating a relational operator. */
internal typealias RelationThunkEnv = (EvaluatorState) -> RelationIterator

/**
 * Invokes [t] with error handling like is supplied by [ThunkFactory].
 *
 * This function is not currently in [ThunkFactory] to avoid complicating it further.  If a need arises, it could be
 * moved.
 */
internal inline fun relationThunk(metas: MetaContainer, crossinline t: RelationThunkEnv): RelationThunkEnv {
    val sourceLocationMeta = metas[SourceLocationMeta.TAG] as? SourceLocationMeta
    return { env: EvaluatorState ->
        try {
            t(env)
        } catch (e: EvaluationException) {
            // Only add source location data to the error context if it doesn't already exist
            // in [errorContext].
            if (!e.errorContext.hasProperty(Property.LINE_NUMBER)) {
                sourceLocationMeta?.let { fillErrorContext(e.errorContext, sourceLocationMeta) }
            }
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "<NO MESSAGE>"
            throw EvaluationException(
                "Generic exception, $message",
                errorCode = ErrorCode.EVALUATOR_GENERIC_EXCEPTION,
                errorContext = errorContextFrom(sourceLocationMeta),
                cause = e,
                internal = true
            )
        }
    }
}
