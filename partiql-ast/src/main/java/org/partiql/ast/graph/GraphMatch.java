package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph match. See Graph Match Nodes — <a href="https://partiql.org/gpml/graph_query.html">specification</a>.
 * <p>
 * Note: this is an experimental API and subject to change without prior notice.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class GraphMatch extends AstNode {
    @NotNull
    private final List<GraphPattern> patterns;

    @Nullable
    private final GraphSelector selector;

    public GraphMatch(@NotNull List<GraphPattern> patterns, @Nullable GraphSelector selector) {
        this.patterns = patterns;
        this.selector = selector;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>(patterns);
        if (selector != null) {
            kids.add(selector);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitGraphMatch(this, ctx);
    }

    @NotNull
    public List<GraphPattern> getPatterns() {
        return this.patterns;
    }

    @Nullable
    public GraphSelector getSelector() {
        return this.selector;
    }
}
