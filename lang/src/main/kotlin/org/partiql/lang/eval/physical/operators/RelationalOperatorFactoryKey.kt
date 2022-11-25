package org.partiql.lang.eval.physical.operators

/**
 * A unique identifier for physical operator factories.
 *
 * Allows all [RelationalOperatorFactory] instances to be stored in a `Map<OperatorFactoryKey, OperatorFactory>`.
 */
data class RelationalOperatorFactoryKey(
    /** The operator implemented by the [RelationalOperatorFactory]. */
    val operator: RelationalOperatorKind,
    /** The name of the operator. */
    val name: String
)
