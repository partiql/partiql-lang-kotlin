package org.partiql.lang.eval.physical.operators

/**
 * A unique identifier for physical operator factories.
 *
 * Allows all [PhysicalOperatorFactory] instances to be stored in a `Map<OperatorFactoryKey, OperatorFactory>`.
 */
data class PhysicalOperatorFactoryKey(
    /** The operator implemented by the [PhysicalOperatorFactory]. */
    val operator: PhysicalOperatorKind,
    /** The name of the operator. */
    val name: String
)
