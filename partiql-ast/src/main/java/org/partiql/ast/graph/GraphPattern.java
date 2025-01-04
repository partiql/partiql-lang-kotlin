package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * A graph pattern.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class GraphPattern extends AstNode {
    @Nullable
    private final GraphRestrictor restrictor;

    @Nullable
    private final Expr prefilter;

    @Nullable
    private final String variable;

    @Nullable
    private final GraphQuantifier quantifier;

    @NotNull
    private final List<GraphPart> parts;

    public GraphPattern(@Nullable GraphRestrictor restrictor, @Nullable Expr prefilter,
                        @Nullable String variable, @Nullable GraphQuantifier quantifier,
                        @NotNull List<GraphPart> parts) {
        this.restrictor = restrictor;
        this.prefilter = prefilter;
        this.variable = variable;
        this.quantifier = quantifier;
        this.parts = parts;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        if (prefilter != null) {
            kids.add(prefilter);
        }
        if (quantifier != null) {
            kids.add(quantifier);
        }
        kids.addAll(parts);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitGraphPattern(this, ctx);
    }

    @Nullable
    public GraphRestrictor getRestrictor() {
        return this.restrictor;
    }

    @Nullable
    public Expr getPrefilter() {
        return this.prefilter;
    }

    @Nullable
    public String getVariable() {
        return this.variable;
    }

    @Nullable
    public GraphQuantifier getQuantifier() {
        return this.quantifier;
    }

    @NotNull
    public List<GraphPart> getParts() {
        return this.parts;
    }
}
