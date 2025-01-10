package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents SQL:1999's row value constructor. EBNF:
 * <code>
 * &lt;row value constructor&gt; ::=
 * &lt;row value constructor element&gt;
 * | [ ROW ] &lt;left paren&gt; &lt;row value constructor element list&gt; &lt;right paren&gt;
 * | &lt;row subquery&gt;
 * </code>
 * This specifically models the second variant where the keyword {@code ROW} is used.
 */
@lombok.Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprRowValue extends Expr {
    private final boolean explicit;

    @NotNull
    private final List<Expr> values;

    /**
     * By default, {@link ExprRowValue#explicit} is false.
     *
     * @param values list of values in the row value constructor.
     */
    public ExprRowValue(@NotNull List<Expr> values) {
        this.explicit = false;
        this.values = values;
    }

    public ExprRowValue(boolean explicit, @NotNull List<Expr> values) {
        this.explicit = explicit;
        this.values = values;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(values);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprRowValue(this, ctx);
    }

    /**
     * Specifies whether the ROW keyword explicitly precedes the elements in the textual representation. For example,
     * {@code ROW (1, 2, 3)} versus {@code (1, 2, 3)}. In the first example, {@code isExplicit} is true.
     */
    public boolean isExplicit() {
        return this.explicit;
    }

    @NotNull
    public List<Expr> getValues() {
        return this.values;
    }
}
