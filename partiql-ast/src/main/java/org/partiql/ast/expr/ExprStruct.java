package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's struct constructor. E.g. {@code {'a': 1, 'b': 2, 'c': 3}}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprStruct extends Expr {
    @NotNull
    private final List<Field> fields;

    public ExprStruct(@NotNull List<Field> fields) {
        this.fields = fields;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(fields);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprStruct(this, ctx);
    }

    @NotNull
    public List<Field> getFields() {
        return this.fields;
    }

    /**
     * Represents a single field of a struct constructor. E.g. {@code 'a': 1}.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Field extends AstNode {
        @NotNull
        private final Expr name;

        @NotNull
        private final Expr value;

        public Field(@NotNull Expr name, @NotNull Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            kids.add(value);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprStructField(this, ctx);
        }

        @NotNull
        public Expr getName() {
            return this.name;
        }

        @NotNull
        public Expr getValue() {
            return this.value;
        }
    }
}
