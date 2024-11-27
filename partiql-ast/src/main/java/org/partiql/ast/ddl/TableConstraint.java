package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class TableConstraint extends AstNode {

    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Unique extends TableConstraint {
        @Nullable
        public final String name;

        @NotNull
        public final List<Identifier> columns;

        public Unique(String name, @NotNull List<Identifier> column) {
            this.name = name;
            this.columns = column;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>(columns);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUnique(this, ctx);
        }
    }

    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class PrimaryKey extends TableConstraint {
        @Nullable
        public final String name;

        @NotNull
        public final List<Identifier> columns;

        public PrimaryKey(String name, @NotNull List<Identifier> column) {
            this.name = name;
            this.columns = column;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>(columns);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPrimaryKey(this, ctx);
        }
    }

    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    // TODO: Table Level Check Constraint not yet supported in the grammar
    public static class Check extends TableConstraint {
        @Nullable
        public final String name;
        @NotNull
        public final Expr searchCondition;

        public Check(String name, @NotNull Expr searchCondition) {
            this.name = name;
            this.searchCondition = searchCondition;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            ArrayList<AstNode> kids = new ArrayList<>();
            kids.add(searchCondition);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitCheck(this, ctx);
        }
    }
}
