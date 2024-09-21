package org.partiql.ast.v1.expr

import com.amazon.ionelement.api.IonElement
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprIon(
    @JvmField
    public var `value`: IonElement,
) : Expr() {
    public override fun children(): List<Expr> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprIon(this, ctx)
}
