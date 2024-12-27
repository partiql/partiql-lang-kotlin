package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ColumnDefinition extends AstNode {
    @NotNull
    @Getter
    private final Identifier name;

    @NotNull
    @Getter
    private final DataType dataType;

    @Getter
    private final boolean optional;

    @NotNull
    @Getter
    private final List<AttributeConstraint> constraints;

    @Nullable
    @Getter
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
}
