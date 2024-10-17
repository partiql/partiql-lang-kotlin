package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.Classification
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.Severity

/**
 * @param location see [PError.location]
 * @return an error representing [PError.ALWAYS_MISSING]
 */
internal class AlwaysMissing(
    location: SourceLocation?
) : PError(
    ALWAYS_MISSING,
    Severity.WARNING(),
    Classification.SEMANTIC(),
    location,
    null
)
