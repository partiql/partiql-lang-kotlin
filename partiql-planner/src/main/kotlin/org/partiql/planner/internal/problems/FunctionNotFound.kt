package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.types.PType

/**
 * @param location see [PError.location]
 * @param fnId see [PError.FUNCTION_NOT_FOUND]
 * @param argTypes see [PError.FUNCTION_NOT_FOUND]
 * @return an error representing [PError.FUNCTION_NOT_FOUND]
 */
internal class FunctionNotFound(
    location: SourceLocation?,
    fnId: Identifier?,
    argTypes: List<PType?>?
) : PError(
    FUNCTION_NOT_FOUND,
    Severity.ERROR(),
    PErrorKind.SEMANTIC(),
    location,
    mapOf(
        "FN_ID" to fnId,
        "ARG_TYPES" to argTypes
    )
)
