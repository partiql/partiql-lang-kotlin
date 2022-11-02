package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.util.propertyValueMapOf

/** Provides a common interface controlling the evaluation-time error signaling of [CompileOptions.typingMode]. */
internal interface ErrorSignaler {
    /** Depending on the error mode, either throws an [EvaluationException] using the specified [ErrorDetails]
     * or returns `MISSING`. */
    fun error(errorCode: ErrorCode, createErrorDetails: () -> ErrorDetails): ExprValue
}

/**
 * Syntactic sugar for error signaling according to the current [TypingMode].
 *
 * Depending on the [TypingMode] mode, if [test] is true, either [createErrorDetails] will be
 * invoked and used to instantiate and throw an [EvaluationException], or the PartiQL `MISSING`
 * value will be returned, depending on the [TypingMode].
 *
 * If [test] is false, [otherwise] is invoked.  Any exception thrown within [otherwise] is left alone
 * to propagate up as usual.  Be aware that this can *still* mean that the current thunk can result in
 * `MISSING`, depending on the [ThunkFactory] currently in use.
 */
internal inline fun ErrorSignaler.errorIf(
    test: Boolean,
    errorCode: ErrorCode,
    crossinline createErrorDetails: () -> ErrorDetails,
    crossinline otherwise: () -> ExprValue
): ExprValue =
    when {
        test -> this.error(errorCode) { createErrorDetails() }
        else -> otherwise()
    }

/**
 * Contains the details of an error.
 *
 * [errorCode] and [errorContext] together are used to compose an error message for the end-user
 * while [message] is meant for to help developers of services that use PartiQL.
 */
internal class ErrorDetails(
    /** Meta information of the node that is to blame for the error. */
    val metas: MetaContainer,
    /** The programmer readable exception message.  */
    val message: String,
    val errorContext: PropertyValueMap? = null
)

internal fun TypingMode.createErrorSignaler() =
    when (this) {
        TypingMode.LEGACY -> LegacyErrorSignaler()
        TypingMode.PERMISSIVE -> PermissiveErrorSignaler(missingExprValue())
    }

/** Defines legacy error signaling. */
private class LegacyErrorSignaler : ErrorSignaler {
    /** Invokes [createErrorDetails] and uses the return value to construct and throw an [EvaluationException]. */
    override fun error(errorCode: ErrorCode, createErrorDetails: () -> ErrorDetails): ExprValue =
        throwEE(errorCode, createErrorDetails)
}

/** Defines permissive error signaling. */
private class PermissiveErrorSignaler(private val theMissingValue: ExprValue) : ErrorSignaler {

    /** Ignores [createErrorDetails] and simply returns [theMissingValue]. */
    override fun error(errorCode: ErrorCode, createErrorDetails: () -> ErrorDetails): ExprValue =
        when (errorCode.errorBehaviorInPermissiveMode) {
            ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> throwEE(errorCode, createErrorDetails)
            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> theMissingValue
        }
}

/** Throws an [EvaluationException] using the specified error details. */
private fun throwEE(errorCode: ErrorCode, createErrorDetails: () -> ErrorDetails): Nothing {
    with(createErrorDetails()) {
        // Add source location if we need to and if we can
        val srcLoc = metas[SourceLocationMeta.TAG] as? SourceLocationMeta
        val errCtx = this.errorContext ?: propertyValueMapOf()
        if (srcLoc != null) {
            if (!errCtx.hasProperty(Property.LINE_NUMBER)) {
                errCtx[Property.LINE_NUMBER] = srcLoc.lineNum
            }
            if (!errCtx.hasProperty(Property.COLUMN_NUMBER)) {
                errCtx[Property.COLUMN_NUMBER] = srcLoc.charOffset
            }
        }

        throw EvaluationException(
            message = message,
            errorCode = errorCode,
            errorContext = errCtx,
            cause = null,
            internal = false
        )
    }
}
