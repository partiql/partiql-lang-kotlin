package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

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

    public final List<ColumnDefinition> columns;

    public final List<TableConstraint> constraints;

    public final Options.PartitionBy partitionBy;

    public final List<Options.KeyValue> tableProperties;

    public CreateTable(@NotNull IdentifierChain name, List<ColumnDefinition> columns, List<TableConstraint> constraints, Options.PartitionBy partitionBy, List<Options.KeyValue> tableProperties) {
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
