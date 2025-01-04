package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's IS &lt;type&gt; predicate. E.g. {@code foo IS INTEGER}.
 * <p>
 * Note: this is an experimental API. Class's fields and behavior may change in a subsequent release.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprIsType extends Expr {
    @NotNull
    private final Expr value;

    @NotNull
    private final DataType type;

    private final boolean not;

    public ExprIsType(@NotNull Expr value, @NotNull DataType type, boolean not) {
        this.value = value;
        this.type = type;
        this.not = not;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(type);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprIsType(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    @NotNull
    public DataType getType() {
        return this.type;
    }

    public boolean isNot() {
        return this.not;
    }
}
