package org.partiql.lang.eval.physical.operators

/**
 * Marker interface with unique [key], allowing all [PhysicalOperatorFactory] implementations to exist in a
 * `Map<OperatorFactoryKey, OperatorFactory>`.
 */
interface PhysicalOperatorFactory {
    val key: PhysicalOperatorFactoryKey
}
