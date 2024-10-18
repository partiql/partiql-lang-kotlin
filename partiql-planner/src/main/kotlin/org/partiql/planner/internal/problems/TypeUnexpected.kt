package org.partiql.planner.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.types.PType

/**
 * @param location see [PError.location]
 * @param actualType see [PError.TYPE_UNEXPECTED]
 * @param expectedTypes see [PError.TYPE_UNEXPECTED]
 * @return an error representing [PError.TYPE_UNEXPECTED]
 */
internal class TypeUnexpected(
    location: SourceLocation?,
    actualType: PType?,
    expectedTypes: List<PType?>?
) : PError(
    TYPE_UNEXPECTED,
    Severity.WARNING(),
    PErrorKind.SEMANTIC(),
    location,
    mapOf("ACTUAL_TYPE" to actualType, "EXPECTED_TYPES" to expectedTypes)
)
