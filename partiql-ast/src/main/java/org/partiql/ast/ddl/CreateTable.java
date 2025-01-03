package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's CREATE TABLE statement.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class CreateTable extends Statement {

    @NotNull
    @Getter
    private final IdentifierChain name;

    @NotNull
    @Getter
    private final List<ColumnDefinition> columns;

    @NotNull
    @Getter
    private final List<TableConstraint> constraints;

    @Nullable
    @Getter
    private final PartitionBy partitionBy;

    @NotNull
    @Getter
    private final List<KeyValue> tableProperties;

    public CreateTable(
            @NotNull IdentifierChain name,
            @NotNull List<ColumnDefinition> columns,
            @NotNull List<TableConstraint> constraints,
            @Nullable PartitionBy partitionBy,
            @NotNull List<KeyValue> tableProperties) {
        this.name = name;
        this.columns = columns;
        this.constraints = constraints;
        this.partitionBy = partitionBy;
        this.tableProperties = tableProperties;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        kids.addAll(columns);
        kids.addAll(constraints);
        if (partitionBy != null) kids.add(partitionBy);
        kids.addAll(tableProperties);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitCreateTable(this, ctx);
    }
}
