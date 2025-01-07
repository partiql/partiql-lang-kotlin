package org.partiql.ast.ddl;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an attribute level constraint.
 */
public abstract class AttributeConstraint extends AstNode {

    @Nullable
    protected final IdentifierChain name;

    protected AttributeConstraint(@Nullable IdentifierChain name) {
        this.name = name;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        return kids;
    }

    @Nullable
    public IdentifierChain getName() {
        return this.name;
    }

    // NULL & NOT NULL

    /**
     * Represents the {@code NULL} and {@code NOT NULL} attribute level constraints.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Null extends AttributeConstraint {
        private final boolean nullable;

        public Null(@Nullable IdentifierChain name, boolean nullable) {
            super(name);
            this.nullable = nullable;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitNullable(this, ctx);
        }

        public boolean isNullable() {
            return this.nullable;
        }
    }

    // Unique and primary

    /**
     * Represents the {@code UNIQUE} and {@code PRIMARY KEY} attribute level constraints.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends AttributeConstraint {
        private final boolean primaryKey;

        public Unique(@Nullable IdentifierChain name, boolean primaryKey) {
            super(name);
            this.primaryKey = primaryKey;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }

        public boolean isPrimaryKey() {
            return this.primaryKey;
        }
    }

    /**
     * Represents the {@code CHECK} attribute level constraint.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Check extends AttributeConstraint {
        @NotNull
        private final Expr searchCondition;

        public Check(@Nullable IdentifierChain name, @NotNull Expr searchCondition) {
            super(name);
            this.searchCondition = searchCondition;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            kids.add(searchCondition);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitCheck(this, ctx);
        }

        @NotNull
        public Expr getSearchCondition() {
            return this.searchCondition;
        }
    }
}
