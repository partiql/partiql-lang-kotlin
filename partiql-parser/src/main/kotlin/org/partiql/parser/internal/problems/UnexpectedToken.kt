package org.partiql.parser.internal.problems

import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.Classification
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.Severity

/**
 * @param location see [PError.location]
 * @param tokenName  see [PError.UNEXPECTED_TOKEN]
 * @param expectedTokens  see [PError.UNEXPECTED_TOKEN]
 * @return an error representing [PError.UNEXPECTED_TOKEN]
 */
// TODO: Add the token text
// TODO: Do we want the offending rule?
internal class UnexpectedToken(
    location: SourceLocation?,
    tokenName: String?,
    expectedTokens: List<String?>?
) : PError(
    UNEXPECTED_TOKEN,
    Severity.ERROR(),
    Classification.SYNTAX(),
    location,
    mapOf(
        "TOKEN_NAME" to tokenName,
        "EXPECTED_TOKENS" to expectedTokens
    )
)
