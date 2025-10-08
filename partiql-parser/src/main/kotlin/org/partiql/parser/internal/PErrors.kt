package org.partiql.parser.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.partiql.spi.SourceLocation
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity

internal object PErrors {

    /**
     * @param location see [PError.location]
     * @param tokenName  see [PError.UNEXPECTED_TOKEN]
     * @param expectedTokens  see [PError.UNEXPECTED_TOKEN]
     * @return an error representing [PError.UNEXPECTED_TOKEN]
     */
    // TODO: Add the token text
    // TODO: Do we want the offending rule?
    internal fun unexpectedToken(
        location: SourceLocation?,
        tokenName: String?,
        expectedTokens: List<String?>?
    ): PError {
        return PError(
            PError.UNEXPECTED_TOKEN,
            Severity.ERROR(),
            PErrorKind.SYNTAX(),
            location,
            mapOf(
                "TOKEN_NAME" to tokenName,
                "EXPECTED_TOKENS" to expectedTokens
            )
        )
    }

    /**
     * @param location see [PError.location]
     * @param content see [PError.UNRECOGNIZED_TOKEN]
     * @return an error representing [PError.UNRECOGNIZED_TOKEN]
     */
    internal fun unrecognizedToken(
        location: SourceLocation?,
        content: String?
    ): PError {
        return PError(
            PError.UNRECOGNIZED_TOKEN,
            Severity.ERROR(),
            PErrorKind.SYNTAX(),
            location,
            mapOf(
                "CONTENT" to content
            )
        )
    }

    /**
     * @return an error representing [PError.EXPERIMENTAL]
     */
    internal fun experimental(
        feature: String,
        ctx: ParserRuleContext,
    ): PError {
        val location = ctxToLocation(ctx)
        return PError(
            PError.EXPERIMENTAL,
            Severity.WARNING(),
            PErrorKind.SYNTAX(),
            location,
            mapOf(
                "FEATURE" to feature
            )
        )
    }

    private fun ctxToLocation(ctx: ParserRuleContext): SourceLocation? {
        val start = ctx.start
        val stop = ctx.stop
        if (start == null || stop == null) {
            return null
        }
        val length = ctx.sourceInterval.length()
        return SourceLocation(start.line.toLong(), start.charPositionInLine + 1L, length.toLong())
    }
}
