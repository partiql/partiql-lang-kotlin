package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class PartitionBy extends AstNode {
    @NotNull
    @Getter
    private final List<Identifier> columns;

    public PartitionBy(@NotNull List<Identifier> columns) {
        this.columns = columns;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(columns);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitPartitionBy(this, ctx);
    }
}
