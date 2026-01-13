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

    /**
     * Creates an error for datetime field keywords (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND)
     * used in expression contexts where they are reserved tokens.
     *
     * Uses UNEXPECTED_TOKEN error code since the token is valid but not expected in expression context.
     * The ExprError AST node preserves the keyword for target-specific recovery.
     *
     * @param location the source location of the datetime field keyword
     * @param fieldName the name of the datetime field keyword (e.g., "YEAR", "MONTH")
     * @return an error representing [PError.UNEXPECTED_TOKEN]
     */
    internal fun datetimeFieldAsExpression(
        location: SourceLocation?,
        fieldName: String
    ): PError {
        return PError(
            PError.UNEXPECTED_TOKEN,
            Severity.ERROR(),
            PErrorKind.SYNTAX(),
            location,
            mapOf(
                "TOKEN_NAME" to fieldName,
                "EXPECTED_TOKENS" to null
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
