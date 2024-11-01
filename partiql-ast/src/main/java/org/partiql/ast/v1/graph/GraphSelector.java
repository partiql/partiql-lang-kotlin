package org.partiql.ast.v1.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

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
        public Collection<AstNode> children() {
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
        public Collection<AstNode> children() {
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
        public Collection<AstNode> children() {
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
        public final long k;

        public AnyK(long k) {
            this.k = k;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
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
        public Collection<AstNode> children() {
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
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphSelectorShortestKGroup(this, ctx);
        }
    }
}
