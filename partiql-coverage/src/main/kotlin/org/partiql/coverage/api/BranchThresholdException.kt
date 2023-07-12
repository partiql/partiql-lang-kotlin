package org.partiql.coverage.api

public class BranchThresholdException(
    minimum: Double,
    actual: Double
) : PartiQLCoverageException(
    message = "PartiQL Branch Coverage Minimum set to $minimum, however, $actual was received.",
    cause = null
)