package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprStruct extends Expr {
    @NotNull
    public List<Field> fields;

    public ExprStruct(@NotNull List<Field> fields) {
        this.fields = fields;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return new ArrayList<>(fields);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprStruct(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Field extends AstNode {
        @NotNull
        public Expr name;

        @NotNull
        public Expr value;

        public Field(@NotNull Expr name, @NotNull Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            kids.add(value);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprStructField(this, ctx);
        }
    }
}
