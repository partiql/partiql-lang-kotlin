package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphSelector extends AstNode {
    /**
     * TODO docs, equals, hashcode
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
     * TODO docs, equals, hashcode
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
     * TODO docs, equals, hashcode
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
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class AnyK extends GraphSelector {
        @Getter
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
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class ShortestK extends GraphSelector {
        public final long k;

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
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class ShortestKGroup extends GraphSelector {
        public final long k;

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
    }
}
