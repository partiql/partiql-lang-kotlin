package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import com.amazon.ionelement.api.IonElement;

import java.util.Collections;
import java.util.Collection;

/**
 * TODO docs, equals, hashcode
 */
public class ExprIon extends Expr {
    @NotNull
    public IonElement value;

    public ExprIon(@NotNull IonElement value) {
        this.value = value;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprIon(this, ctx);
    }
}
