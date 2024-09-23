package org.partiql.ast.v1.graph;

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
public class GraphPattern extends AstNode {
    @Nullable
    public GraphRestrictor restrictor;

    @Nullable
    public Expr prefilter;

    @Nullable
    public String variable;

    @Nullable
    public GraphQuantifier quantifier;

    @NotNull
    public List<GraphPart> parts;

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
        return visitor.visitGraphMatchPattern(this, ctx);
    }
}
