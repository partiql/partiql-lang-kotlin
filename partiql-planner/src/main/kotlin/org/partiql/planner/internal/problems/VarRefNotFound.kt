package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity

/**
 * @param location see [PError.location]
 * @param id see [PError.VAR_REF_NOT_FOUND]
 * @param locals see [PError.VAR_REF_NOT_FOUND]
 * @return an error representing [PError.VAR_REF_NOT_FOUND]
 */
internal class VarRefNotFound(
    location: SourceLocation?,
    id: Identifier?,
    locals: List<String?>?
) : PError(
    VAR_REF_NOT_FOUND,
    Severity.WARNING(),
    PErrorKind.SEMANTIC(),
    location,
    mapOf("ID" to id, "LOCALS" to locals)
)
