package org.partiql.ast.v1

import kotlin.random.Random

/**
 * TODO docs, equals, hashcode
 */
public abstract class AstNode {
    @JvmField
    public var tag: String = "Ast-${"%06x".format(Random.nextInt())}"

    public abstract fun children(): Collection<AstNode>

    public abstract fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R
}
