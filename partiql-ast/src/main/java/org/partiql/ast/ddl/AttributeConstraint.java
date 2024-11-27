package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.expr.Expr;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public abstract class AttributeConstraint extends AstNode {

    // NULL & NOT NULL

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Null extends AttributeConstraint {
        @Nullable
        public final String name;

        @NotNull
        public final Boolean isNullable;

        public Null(@Nullable String name, @NotNull Boolean isNullable) {
            this.name = name;
            this.isNullable = isNullable;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return Collections.emptyList();
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
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends AttributeConstraint {
        @Nullable
        public final String name;

        @NotNull
        public final Boolean isPrimary;

        public Unique(@Nullable String name, @NotNull Boolean isPrimary) {
            this.name = name;
            this.isPrimary = isPrimary;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Check extends AttributeConstraint {
        @Nullable
        public final String name;

        @NotNull
        public final Expr searchCondition;

        public Check(@Nullable String name, @NotNull Expr searchCondition) {
            this.name = name;
            this.searchCondition = searchCondition;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitCheck(this, ctx);
        }
    }
}
