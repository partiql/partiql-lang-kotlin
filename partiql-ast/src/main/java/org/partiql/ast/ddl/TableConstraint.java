package org.partiql.ast.ddl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for table constraints.
 */
public abstract class TableConstraint extends AstNode {
    @Nullable
    @Getter
    private final IdentifierChain name;

    protected TableConstraint(@Nullable IdentifierChain name) {
        this.name = name;
    }

    /**
     * Represents CREATE TABLE's UNIQUE constraint.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends TableConstraint {
        @NotNull
        @Getter
        private final List<Identifier> columns;

        @Getter
        private final boolean primaryKey;

        public Unique(@Nullable IdentifierChain name, @NotNull List<Identifier> column, boolean primaryKey) {
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
    }
}
