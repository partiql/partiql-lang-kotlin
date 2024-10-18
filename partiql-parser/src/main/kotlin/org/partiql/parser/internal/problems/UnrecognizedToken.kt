package org.partiql.parser.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity

/**
 * @param location see [PError.location]
 * @param content see [PError.UNRECOGNIZED_TOKEN]
 * @return an error representing [PError.UNRECOGNIZED_TOKEN]
 */
public class UnrecognizedToken(
    location: SourceLocation?,
    content: String?
) : PError(
    UNRECOGNIZED_TOKEN,
    Severity.ERROR(),
    PErrorKind.SYNTAX(),
    location,
    mapOf(
        "CONTENT" to content
    )
)
