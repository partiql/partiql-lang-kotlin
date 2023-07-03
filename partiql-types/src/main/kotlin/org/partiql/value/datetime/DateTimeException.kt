package org.partiql.value.datetime

/**
 * This is the overall exception an application should catch during run-time.
 */
public class DateTimeException(
    public override val message: String? = null,
    public override val cause: Throwable? = null
) : RuntimeException()
