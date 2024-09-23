package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Expr extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof ExprLit) {
            return visitor.visitExprLit((ExprLit) this, ctx);
        } else if (this instanceof ExprIon) {
            return visitor.visitExprIon((ExprIon) this, ctx);
        } else if (this instanceof ExprVar) {
            return visitor.visitExprVar((ExprVar) this, ctx);
        } else if (this instanceof ExprSessionAttribute) {
            return visitor.visitExprSessionAttribute((ExprSessionAttribute) this, ctx);
        } else if (this instanceof ExprPath) {
            return visitor.visitExprPath((ExprPath) this, ctx);
        } else if (this instanceof ExprCall) {
            return visitor.visitExprCall((ExprCall) this, ctx);
        } else if (this instanceof ExprParameter) {
            return visitor.visitExprParameter((ExprParameter) this, ctx);
        } else if (this instanceof ExprOperator) {
            return visitor.visitExprOperator((ExprOperator) this, ctx);
        } else if (this instanceof ExprNot) {
            return visitor.visitExprNot((ExprNot) this, ctx);
        } else if (this instanceof ExprAnd) {
            return visitor.visitExprAnd((ExprAnd) this, ctx);
        } else if (this instanceof ExprOr) {
            return visitor.visitExprOr((ExprOr) this, ctx);
        } else if (this instanceof ExprValues) {
            return visitor.visitExprValues((ExprValues) this, ctx);
        } else if (this instanceof ExprCollection) {
            return visitor.visitExprCollection((ExprCollection) this, ctx);
        } else if (this instanceof ExprStruct) {
            return visitor.visitExprStruct((ExprStruct) this, ctx);
        } else if (this instanceof ExprLike) {
            return visitor.visitExprLike((ExprLike) this, ctx);
        } else if (this instanceof ExprBetween) {
            return visitor.visitExprBetween((ExprBetween) this, ctx);
        } else if (this instanceof ExprInCollection) {
            return visitor.visitExprInCollection((ExprInCollection) this, ctx);
        } else if (this instanceof ExprIsType) {
            return visitor.visitExprIsType((ExprIsType) this, ctx);
        } else if (this instanceof ExprCase) {
            return visitor.visitExprCase((ExprCase) this, ctx);
        } else if (this instanceof ExprCoalesce) {
            return visitor.visitExprCoalesce((ExprCoalesce) this, ctx);
        } else if (this instanceof ExprNullIf) {
            return visitor.visitExprNullIf((ExprNullIf) this, ctx);
        } else if (this instanceof ExprSubstring) {
            return visitor.visitExprSubstring((ExprSubstring) this, ctx);
        } else if (this instanceof ExprPosition) {
            return visitor.visitExprPosition((ExprPosition) this, ctx);
        } else if (this instanceof ExprTrim) {
            return visitor.visitExprTrim((ExprTrim) this, ctx);
        } else if (this instanceof ExprOverlay) {
            return visitor.visitExprOverlay((ExprOverlay) this, ctx);
        } else if (this instanceof ExprExtract) {
            return visitor.visitExprExtract((ExprExtract) this, ctx);
        } else if (this instanceof ExprCast) {
            return visitor.visitExprCast((ExprCast) this, ctx);
        } else if (this instanceof ExprDateAdd) {
            return visitor.visitExprDateAdd((ExprDateAdd) this, ctx);
        } else if (this instanceof ExprDateDiff) {
            return visitor.visitExprDateDiff((ExprDateDiff) this, ctx);
        } else if (this instanceof ExprQuerySet) {
            return visitor.visitExprQuerySet((ExprQuerySet) this, ctx);
        } else if (this instanceof ExprMatch) {
            return visitor.visitExprMatch((ExprMatch) this, ctx);
        } else if (this instanceof ExprWindow) {
            return visitor.visitExprWindow((ExprWindow) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
