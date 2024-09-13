package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class SetOp(
    @JvmField
    public var type: Type,
    @JvmField
    public var setq: SetQuantifier?,
) : AstNode() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitSetOp(
        this,
        ctx
    )

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Type {
        UNION,
        INTERSECT,
        EXCEPT,
        OTHER,
    }
}
