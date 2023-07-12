package org.partiql.coverage.api

public sealed class PartiQLCoverageException(
    message: String?,
    cause: Throwable?
) : Exception(message, cause)