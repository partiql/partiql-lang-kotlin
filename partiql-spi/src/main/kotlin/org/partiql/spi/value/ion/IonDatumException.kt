package org.partiql.spi.value.ion

import com.amazon.ion.Span

/**
 * These are errors specific to reading Ion data.
 *
 * TODO add DATA to PError kind.
 */
public class IonDatumException internal constructor(
    public override val message: String,
    public override val cause: Throwable?,
    public val span: Span,
) : RuntimeException()
