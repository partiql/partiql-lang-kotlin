package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Constraint(
    @JvmField
    public var name: String?,
    @JvmField
    public var constraintBody: ConstraintBody,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(constraintBody)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitConstraint(this, ctx)
}
