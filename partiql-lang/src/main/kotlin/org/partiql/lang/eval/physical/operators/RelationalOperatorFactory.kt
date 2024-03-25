package org.partiql.lang.eval.physical.operators

/**
 * Marker interface with unique [key], which allows all [RelationalOperatorFactory] implementations to exist in a
 * `Map<OperatorFactoryKey, OperatorFactory>`.
 *
 * Implementations of this interface also define a `create` function, each with a different signature, but always
 * returning an instance of [RelationExpression], which is ready to be evaluated as part of query evaluation. Within
 * the `create` function, the factory factory may access any values placed in its
 * [org.partiql.lang.domains.PartiqlPhysical.Impl.staticArgs], which may be relevant to the operator's implementation
 * and perform any compile-time initialization of the [RelationExpression].
 */
interface RelationalOperatorFactory {
    val key: RelationalOperatorFactoryKey
}
