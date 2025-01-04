package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single step in a PartiQL exclude path.
 *
 * @see ExcludePath
 */
public abstract class ExcludeStep extends AstNode {
    /**
     * Exclude step on a struct field. E.g. {@code EXCLUDE t.a.b}.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class StructField extends ExcludeStep {
        @NotNull
        private final Identifier symbol;

        public StructField(@NotNull Identifier symbol) {
            this.symbol = symbol;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(symbol);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepStructField(this, ctx);
        }

        @NotNull
        public Identifier getSymbol() {
            return this.symbol;
        }
    }

    /**
     * Exclude step on a collection index. E.g. {@code EXCLUDE t.a[1]}
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class CollIndex extends ExcludeStep {
        private final int index;

        public CollIndex(int index) {
            this.index = index;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepCollIndex(this, ctx);
        }

        public int getIndex() {
            return this.index;
        }
    }

    /**
     * Exclude step on a struct wildcard. E.g. {@code EXCLUDE t.a.*}
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class StructWildcard extends ExcludeStep {
        public StructWildcard() {}

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepStructWildcard(this, ctx);
        }
    }

    /**
     * Exclude step on a collection wildcard. E.g. {@code EXCLUDE t.a[*]}
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class CollWildcard extends ExcludeStep {
        public CollWildcard() {}

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExcludeStepCollWildcard(this, ctx);
        }
    }
}
