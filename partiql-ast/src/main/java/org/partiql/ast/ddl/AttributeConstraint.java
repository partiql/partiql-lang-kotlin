package org.partiql.ast.ddl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class AttributeConstraint extends AstNode {

    @Nullable
    @Getter
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

    // NULL & NOT NULL

    /**
     * TODO docs, equals, hashcode
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Null extends AttributeConstraint {
        @Getter
        private final boolean nullable;

        public Null(@Nullable IdentifierChain name, boolean nullable) {
            super(name);
            this.nullable = nullable;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitNullable(this, ctx);
        }
    }

    // Unique and primary

    /**
     * TODO docs, equals, hashcode
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends AttributeConstraint {
        @Getter
        private final boolean primaryKey;

        public Unique(@Nullable IdentifierChain name, boolean primaryKey) {
            super(name);
            this.primaryKey = primaryKey;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Check extends AttributeConstraint {
        @NotNull
        @Getter
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
    }
}
