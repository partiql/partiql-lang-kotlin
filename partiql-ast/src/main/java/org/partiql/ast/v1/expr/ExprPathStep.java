package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class ExprPathStep extends AstNode {

    /**
     * TODO docs, equals, hashcode
     */
    public static class Symbol extends ExprPathStep {
        @NotNull
        public Identifier.Symbol symbol;

        public Symbol(@NotNull Identifier.Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprPathStepSymbol(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Index extends ExprPathStep {
        @NotNull
        public Expr key;

        public Index(@NotNull Expr key) {
            this.key = key;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(key);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprPathStepIndex(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Wildcard extends ExprPathStep {
        @Override
        @NotNull
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprPathStepWildcard(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Unpivot extends ExprPathStep {
        @Override
        @NotNull
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprPathStepUnpivot(this, ctx);
        }
    }
}
