package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.Classification
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.Severity

/**
 * @param location see [PError.location]
 * @return an error representing [PError.PATH_INDEX_NEVER_SUCCEEDS]
 */
internal class PathIndexNeverSucceeds(
    location: SourceLocation?
) : PError(
    PATH_INDEX_NEVER_SUCCEEDS,
    Severity.WARNING(),
    Classification.SEMANTIC(),
    location,
    null
)
