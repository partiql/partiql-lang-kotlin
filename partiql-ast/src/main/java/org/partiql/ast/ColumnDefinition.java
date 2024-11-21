package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ColumnDefinition extends AstNode {

    @NotNull
    private final Identifier name;

    @NotNull
    private final DataType dataType;

    @NotNull
    private final Boolean isOptional;

    private final List<AttributeConstraint> constraints;

    private final String comment;

    public ColumnDefinition(@NotNull Identifier name, @NotNull DataType dataType, @NotNull Boolean isOptional, List<AttributeConstraint> constraints, String comment) {
        this.name = name;
        this.dataType = dataType;
        this.isOptional = isOptional;
        this.constraints = constraints;
        this.comment = comment;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        kids.add(dataType);
        if (constraints != null) kids.addAll(constraints);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitColumnDefinition(this, ctx);
    }
}
