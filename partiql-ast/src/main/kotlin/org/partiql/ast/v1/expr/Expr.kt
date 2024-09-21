package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public abstract class Expr : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is ExprLit -> visitor.visitExprLit(this, ctx)
        is ExprIon -> visitor.visitExprIon(this, ctx)
        is ExprVar -> visitor.visitExprVar(this, ctx)
        is ExprSessionAttribute -> visitor.visitExprSessionAttribute(this, ctx)
        is ExprPath -> visitor.visitExprPath(this, ctx)
        is ExprCall -> visitor.visitExprCall(this, ctx)
        is ExprParameter -> visitor.visitExprParameter(this, ctx)
        is ExprOperator -> visitor.visitExprOperator(this, ctx)
        is ExprNot -> visitor.visitExprNot(this, ctx)
        is ExprAnd -> visitor.visitExprAnd(this, ctx)
        is ExprOr -> visitor.visitExprOr(this, ctx)
        is ExprValues -> visitor.visitExprValues(this, ctx)
        is ExprCollection -> visitor.visitExprCollection(this, ctx)
        is ExprStruct -> visitor.visitExprStruct(this, ctx)
        is ExprLike -> visitor.visitExprLike(this, ctx)
        is ExprBetween -> visitor.visitExprBetween(this, ctx)
        is ExprInCollection -> visitor.visitExprInCollection(this, ctx)
        is ExprIsType -> visitor.visitExprIsType(this, ctx)
        is ExprCase -> visitor.visitExprCase(this, ctx)
        is ExprCoalesce -> visitor.visitExprCoalesce(this, ctx)
        is ExprNullIf -> visitor.visitExprNullIf(this, ctx)
        is ExprSubstring -> visitor.visitExprSubstring(this, ctx)
        is ExprPosition -> visitor.visitExprPosition(this, ctx)
        is ExprTrim -> visitor.visitExprTrim(this, ctx)
        is ExprOverlay -> visitor.visitExprOverlay(this, ctx)
        is ExprExtract -> visitor.visitExprExtract(this, ctx)
        is ExprCast -> visitor.visitExprCast(this, ctx)
        is ExprDateAdd -> visitor.visitExprDateAdd(this, ctx)
        is ExprDateDiff -> visitor.visitExprDateDiff(this, ctx)
        is ExprQuerySet -> visitor.visitExprQuerySet(this, ctx)
        is ExprMatch -> visitor.visitExprMatch(this, ctx)
        is ExprWindow -> visitor.visitExprWindow(this, ctx)
        else -> throw NotImplementedError()
    }
}
