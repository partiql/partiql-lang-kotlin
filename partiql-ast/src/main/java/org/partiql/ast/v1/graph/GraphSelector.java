package org.partiql.ast.v1.graph;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphSelector extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof AnyShortest) {
            return visitor.visitGraphSelectorAnyShortest((AnyShortest) this, ctx);
        } else if (this instanceof AllShortest) {
            return visitor.visitGraphSelectorAllShortest((AllShortest) this, ctx);
        } else if (this instanceof Any) {
            return visitor.visitGraphSelectorAny((Any) this, ctx);
        } else if (this instanceof AnyK) {
            return visitor.visitGraphSelectorAnyK((AnyK) this, ctx);
        } else if (this instanceof ShortestK) {
            return visitor.visitGraphSelectorShortestK((ShortestK) this, ctx);
        } else if (this instanceof ShortestKGroup) {
            return visitor.visitGraphSelectorShortestKGroup((ShortestKGroup) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
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
