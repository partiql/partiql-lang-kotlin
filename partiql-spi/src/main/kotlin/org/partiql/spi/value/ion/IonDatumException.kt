package org.partiql.spi.value.ion

import com.amazon.ion.OffsetSpan
import com.amazon.ion.Span
import com.amazon.ion.TextSpan

/**
 * These are errors specific to reading Ion data.
 *
 * TODO add DATA to PError kind.
 */
internal class IonDatumException internal constructor(
    public override val message: String,
    public override val cause: Throwable?,
    public val span: Span?,
) : RuntimeException() {

    public constructor(message: String) : this(message, null, null)

    override fun getLocalizedMessage(): String {
        val loc = when (span) {
            is TextSpan -> "line ${span.startLine}, column ${span.startColumn}"
            is OffsetSpan -> "offset [${span.startOffset};${span.finishOffset}]"
            else -> null
        }
        return if (loc == null) message else "$message at $loc"
    }
}
