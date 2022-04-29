/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.SqlException
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.util.err
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

/** Error for evaluation problems. */
open class EvaluationException(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap? = null,
    cause: Throwable? = null,
    override val internal: Boolean
) : SqlException(message, errorCode, errorContext, cause) {

    constructor(
        cause: Throwable,
        errorCode: ErrorCode,
        errorContext: PropertyValueMap? = null,
        internal: Boolean
    ) : this(
        message = cause.message ?: "<NO MESSAGE>",
        errorCode = errorCode,
        errorContext = errorContext,
        internal = internal,
        cause = cause
    )
}

/**
 * Shorthand for throwing function evaluation. Separated from [err] to avoid loosing the context unintentionally
 */
internal fun errNoContext(message: String, errorCode: ErrorCode, internal: Boolean): Nothing = err(message, errorCode, null, internal)

/** Shorthand for throwing evaluation with context with an error code.. */
internal fun err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap?, internal: Boolean): Nothing =
    throw EvaluationException(message, errorCode, errorContext, internal = internal)

internal fun expectedArgTypeErrorMsg(types: List<ExprValueType>): String = when (types.size) {
    0 -> throw IllegalStateException("Should have at least one expected argument type. ")
    1 -> types[0].toString()
    else -> {
        val window = types.size - 1
        val (most, last) = types.windowed(window, window, true)
        most.joinToString(", ") + ", or ${last.first()}"
    }
}

/** Throw an [ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL] error */
internal fun errInvalidArgumentType(
    signature: FunctionSignature,
    position: Int,
    expectedTypes: List<ExprValueType>,
    actualType: ExprValueType
): Nothing {

    val expectedTypeMsg = expectedArgTypeErrorMsg(expectedTypes)

    val errorContext = propertyValueMapOf(
        Property.FUNCTION_NAME to signature.name,
        Property.EXPECTED_ARGUMENT_TYPES to expectedTypeMsg,
        Property.ARGUMENT_POSITION to position,
        Property.ACTUAL_ARGUMENT_TYPES to actualType.toString()
    )

    err(
        message = "Invalid type for argument $position of ${signature.name}.",
        errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
        errorContext = errorContext,
        internal = false
    )
}

internal fun errIntOverflow(intSizeInBytes: Int, errorContext: PropertyValueMap? = null): Nothing {
    throw EvaluationException(
        message = "Int overflow or underflow",
        errorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        errorContext = (errorContext ?: PropertyValueMap()).also {
            it[Property.INT_SIZE_IN_BYTES] = intSizeInBytes
        },
        internal = false
    )
}

fun errorContextFrom(location: SourceLocationMeta?): PropertyValueMap {
    val errorContext = PropertyValueMap()
    if (location != null) {
        fillErrorContext(errorContext, location)
    }
    return errorContext
}

fun fillErrorContext(errorContext: PropertyValueMap, location: SourceLocationMeta?) {
    if (location != null) {
        errorContext[Property.LINE_NUMBER] = location.lineNum
        errorContext[Property.COLUMN_NUMBER] = location.charOffset
    }
}

fun fillErrorContext(errorContext: PropertyValueMap, metaContainer: MetaContainer) {
    val location = metaContainer[SourceLocationMeta.TAG] as? SourceLocationMeta
    if (location != null) {
        fillErrorContext(errorContext, location)
    }
}

/**
 * Returns the [SourceLocationMeta] as an error context if the [SourceLocationMeta.TAG] exists in the passed
 * [metaContainer]. Otherwise, returns an empty map.
 */
fun errorContextFrom(metaContainer: MetaContainer?): PropertyValueMap {
    if (metaContainer == null) {
        return PropertyValueMap()
    }
    val location = metaContainer[SourceLocationMeta.TAG] as? SourceLocationMeta
    return if (location != null) {
        errorContextFrom(location)
    } else {
        PropertyValueMap()
    }
}
