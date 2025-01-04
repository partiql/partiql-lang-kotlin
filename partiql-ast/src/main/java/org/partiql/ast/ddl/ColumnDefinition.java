package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a column definition in a CREATE TABLE statement.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ColumnDefinition extends AstNode {
    @NotNull
    private final Identifier name;

    @NotNull
    private final DataType dataType;

    private final boolean optional;

    @NotNull
    private final List<AttributeConstraint> constraints;

    @Nullable
    private final String comment;

    public ColumnDefinition(
            @NotNull Identifier name,
            @NotNull DataType dataType,
            boolean optional,
            @NotNull List<AttributeConstraint> constraints,
            @Nullable String comment) {
        this.name = name;
        this.dataType = dataType;
        this.optional = optional;
        this.constraints = constraints;
        this.comment = comment;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        kids.add(dataType);
        kids.addAll(constraints);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitColumnDefinition(this, ctx);
    }

    @NotNull
    public Identifier getName() {
        return this.name;
    }

    @NotNull
    public DataType getDataType() {
        return this.dataType;
    }

    public boolean isOptional() {
        return this.optional;
    }

    @NotNull
    public List<AttributeConstraint> getConstraints() {
        return this.constraints;
    }

    @Nullable
    public String getComment() {
        return this.comment;
    }
}
