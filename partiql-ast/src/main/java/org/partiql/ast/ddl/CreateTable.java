package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class CreateTable extends Ddl {

    @NotNull
    public final IdentifierChain name;

    @NotNull
    public final List<ColumnDefinition> columns;

    @NotNull
    public final List<TableConstraint> constraints;

    @Nullable
    public final PartitionBy partitionBy;

    @NotNull
    public final List<KeyValue> tableProperties;

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
