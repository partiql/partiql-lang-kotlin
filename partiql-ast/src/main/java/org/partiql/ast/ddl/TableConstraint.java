package org.partiql.ast.ddl;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.List;

public abstract class TableConstraint extends AstNode {
    @Nullable
    public final IdentifierChain name;

    protected TableConstraint(@Nullable IdentifierChain name) {
        this.name = name;
    }

    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends TableConstraint {
        @NotNull
        public final List<Identifier> columns;

        public final boolean isPrimaryKey;

        public Unique(@Nullable IdentifierChain name, @NotNull List<Identifier> column, boolean isPrimaryKey) {
            super(name);
            this.columns = column;
            this.isPrimaryKey = isPrimaryKey;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            kids.addAll(columns);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }
    }
}
