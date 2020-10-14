package org.partiql.lang.eval

import org.partiql.lang.ast.MetaContainer
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap


internal class ErrorDetails(
    val metas: MetaContainer,
    val message: String,
    val errorCode: ErrorCode,
    val errorContext: PropertyValueMap? = null
)

internal class StrictnessContext(val strictnessMode: StrictnessMode, val valueFactory: ExprValueFactory) {

    inline fun errorIf(
        test: Boolean,
        crossinline detailsConstructor: () -> ErrorDetails,
        crossinline otherwise: () -> ExprValue
    ): ExprValue =
        when {
            test ->
                when(strictnessMode) {
                    StrictnessMode.STANDARD ->
                        with(detailsConstructor()) {
                            throw EvaluationException(
                                message = message,
                                errorCode = errorCode,
                                errorContext = errorContext,
                                cause = null,
                                internal = false)
                        }
                    StrictnessMode.PERMISSIVE ->
                        valueFactory.missingValue
                }
            else -> otherwise()
        }

    inline fun errorUnless(
        test: Boolean,
        crossinline detailsConstructor: () -> ErrorDetails,
        crossinline otherwise: () -> ExprValue
    ): ExprValue =
        errorIf(
            !test,
            detailsConstructor,
            otherwise)

}

