package org.partiql.coverage.api

public class MinimumThresholdException(
    minimum: Double,
    actual: Double,
    type: ThresholdType
) : PartiQLCoverageException(
    message = "PartiQL $type Coverage Minimum set to $minimum, however, $actual was received.",
    cause = null
) {
    public enum class ThresholdType {
        BRANCH,
        CONDITION
    }
}