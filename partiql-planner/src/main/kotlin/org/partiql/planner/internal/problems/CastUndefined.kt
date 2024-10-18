package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.types.PType

/**
 * @param location see [PError.location]
 * @param inputType  see [PError.UNDEFINED_CAST]
 * @param targetType  see [PError.UNDEFINED_CAST]
 * @return an error representing [PError.UNDEFINED_CAST]
 */
internal class CastUndefined(
    location: SourceLocation?,
    inputType: PType?,
    targetType: PType?
) : PError(
    UNDEFINED_CAST,
    Severity.WARNING(),
    PErrorKind.SEMANTIC(),
    location,
    mapOf(
        "INPUT_TYPE" to inputType,
        "TARGET_TYPE" to targetType
    )
)
