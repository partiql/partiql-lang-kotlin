package org.partiql.ast.ddl;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for table constraints.
 */
public abstract class TableConstraint extends AstNode {
    @Nullable
    private final Identifier name;

    protected TableConstraint(@Nullable Identifier name) {
        this.name = name;
    }

    @Nullable
    public Identifier getName() {
        return this.name;
    }

    /**
     * Represents CREATE TABLE's UNIQUE constraint.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends TableConstraint {
        @NotNull
        private final List<Identifier.Simple> columns;

        private final boolean primaryKey;

        public Unique(@Nullable Identifier name, @NotNull List<Identifier.Simple> column, boolean primaryKey) {
            super(name);
            this.columns = column;
            this.primaryKey = primaryKey;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(getName());
            kids.addAll(columns);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }

        @NotNull
        public List<Identifier.Simple> getColumns() {
            return this.columns;
        }

        public boolean isPrimaryKey() {
            return this.primaryKey;
        }
    }
}
