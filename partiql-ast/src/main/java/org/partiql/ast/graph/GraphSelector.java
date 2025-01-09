package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for graph path selector. See <a href="https://arxiv.org/abs/2112.06217">Fig. 8</a>.
 * <p>
 * Note: this is an experimental API and subject to change without prior notice.
 */
public abstract class GraphSelector extends AstNode {
    /**
     * ANY SHORTEST selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class AnyShortest extends GraphSelector {
        public AnyShortest() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorAnyShortest(this, ctx);
        }
    }

    /**
     * ALL SHORTEST selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class AllShortest extends GraphSelector {
        public AllShortest() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorAllShortest(this, ctx);
        }
    }

    /**
     * ANY selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Any extends GraphSelector {
        public Any() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorAny(this, ctx);
        }
    }

    /**
     * ANY k selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class AnyK extends GraphSelector {
        private final long k;

        public AnyK(long k) {
            this.k = k;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorAnyK(this, ctx);
        }

        public long getK() {
            return this.k;
        }
    }

    /**
     * SHORTEST k selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class ShortestK extends GraphSelector {
        private final long k;

        public ShortestK(long k) {
            this.k = k;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorShortestK(this, ctx);
        }

        public long getK() {
            return this.k;
        }
    }

    /**
     * SHORTEST k GROUP selector.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class ShortestKGroup extends GraphSelector {
        private final long k;

        public ShortestKGroup(long k) {
            this.k = k;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorShortestKGroup(this, ctx);
        }

        public long getK() {
            return this.k;
        }
    }
}
