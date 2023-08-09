package org.partiql.coverage.api.impl

internal class ThresholdException internal constructor(
    minimum: Double,
    actual: Double,
    type: ThresholdType
) : Exception("PartiQL $type Coverage Minimum set to $minimum, however, $actual was received.", null) {
    enum class ThresholdType {
        BRANCH,
        CONDITION
    }
}
