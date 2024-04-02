package org.partiql.shape.errors

public sealed interface Error {
    public val cause: Throwable?
    public val message: String?
}
