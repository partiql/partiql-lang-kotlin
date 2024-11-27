package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class CreateTable extends Ddl {

    @NotNull
    public final IdentifierChain name;

    @Nullable
    public final List<ColumnDefinition> columns;

    @Nullable
    public final List<TableConstraint> constraints;

    @Nullable
    public final PartitionBy partitionBy;

    @Nullable
    public final List<KeyValue> tableProperties;

    public CreateTable(
            @NotNull IdentifierChain name,
            @Nullable List<ColumnDefinition> columns,
            @Nullable List<TableConstraint> constraints,
            @Nullable PartitionBy partitionBy,
            @Nullable List<KeyValue> tableProperties) {
        this.name = name;
        this.columns = columns;
        this.constraints = constraints;
        this.partitionBy = partitionBy;
        this.tableProperties = tableProperties;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        if (columns != null) kids.addAll(columns);
        if (constraints != null) kids.addAll(constraints);
        if (partitionBy != null) kids.add(partitionBy);
        if (tableProperties != null) kids.addAll(tableProperties);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitCreateTable(this, ctx);
    }
}
