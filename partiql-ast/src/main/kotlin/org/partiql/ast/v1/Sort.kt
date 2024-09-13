package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Sort(
    @JvmField
    public var expr: Expr,
    @JvmField
    public var dir: Dir?,
    @JvmField
    public var nulls: Nulls?,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(expr)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitSort(
        this,
        ctx
    )

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Dir {
        ASC,
        DESC,
        OTHER,
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Nulls {
        FIRST,
        LAST,
        OTHER,
    }
}
