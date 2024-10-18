package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.spi.function.Function
import org.partiql.types.PType

/**
 * @param location see [PError.location]
 * @param fnId see [PError.FUNCTION_TYPE_MISMATCH]
 * @param argTypes see [PError.FUNCTION_TYPE_MISMATCH]
 * @return an error representing [PError.FUNCTION_TYPE_MISMATCH]
 */
internal class FunctionTypeMismatch(
    location: SourceLocation?,
    fnId: Identifier?,
    argTypes: List<PType?>?,
    candidates: List<Function?>?
) : PError(
    FUNCTION_TYPE_MISMATCH,
    Severity.WARNING(),
    PErrorKind.SEMANTIC(),
    location,
    mapOf(
        "FN_ID" to fnId,
        "ARG_TYPES" to argTypes,
        "CANDIDATES" to candidates
    )
)
