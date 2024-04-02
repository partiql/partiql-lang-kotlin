package org.partiql.shape.errors

public class InternalError(
    override val message: String? = null,
    override val cause: Throwable? = null
) : Error
