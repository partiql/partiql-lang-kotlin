package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class ExcludeStep extends AstNode {
    /**
     * TODO docs, equals, hashcode
     */
    public static class StructField extends ExcludeStep {
        @NotNull
        public Identifier symbol;

        public StructField(@NotNull Identifier symbol) {
            this.symbol = symbol;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(symbol);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepStructField(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class CollIndex extends ExcludeStep {
        public int index;

        public CollIndex(int index) {
            this.index = index;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepCollIndex(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class StructWildcard extends ExcludeStep {
        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepStructWildcard(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class CollWildcard extends ExcludeStep {
        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepCollWildcard(this, ctx);
        }
    }
}
