package org.partiql.ast.v1.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class GraphPattern extends AstNode {
    @Nullable
    public final GraphRestrictor restrictor;

    @Nullable
    public final Expr prefilter;

    @Nullable
    public final String variable;

    @Nullable
    public final GraphQuantifier quantifier;

    @NotNull
    public final List<GraphPart> parts;

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
    public Collection<AstNode> children() {
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
}
