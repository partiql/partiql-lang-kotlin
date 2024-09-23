package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class PathLit extends AstNode {
    @NotNull
    public Identifier.Symbol root;

    @NotNull
    public List<PathLitStep> steps;

    public PathLit(@NotNull Identifier.Symbol root, @NotNull List<PathLitStep> steps) {
        this.root = root;
        this.steps = steps;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        kids.addAll(steps);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitPathLit(this, ctx);
    }
}
