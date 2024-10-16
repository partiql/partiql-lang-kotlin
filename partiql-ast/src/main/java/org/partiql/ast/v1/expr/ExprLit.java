package org.partiql.ast.v1.expr;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.value.PartiQLValue;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
public class ExprLit extends Expr {
    @NotNull
    public final PartiQLValue value; // This representation be changed in https://github.com/partiql/partiql-lang-kotlin/issues/1589

    public ExprLit(@NotNull PartiQLValue value) {
        this.value = value;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprLit(this, ctx);
    }
}
