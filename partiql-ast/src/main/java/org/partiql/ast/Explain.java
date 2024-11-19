package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.literal.Literal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class Explain extends Statement {
    @NotNull
    public final Map<String, Literal> options;

    @NotNull
    public final Statement statement;

    public Explain(@NotNull Map<String, Literal> options, @NotNull Statement statement) {
        this.options = options;
        this.statement = statement;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(statement);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExplain(this, ctx);
    }
}
