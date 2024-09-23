package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class Constraint extends AstNode {
    @Nullable
    public String name;

    @NotNull
    public ConstraintType constraintType;

    public Constraint(@Nullable String name, @NotNull ConstraintType constraintType) {
        this.name = name;
        this.constraintType = constraintType;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(constraintType);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitConstraint(this, ctx);
    }
}
