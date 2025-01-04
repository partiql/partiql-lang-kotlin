package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents PartiQL's EXPLAIN statement.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Explain extends Statement {
    @NotNull
    private final Map<String, Literal> options;

    @NotNull
    private final Statement statement;

    public Explain(@NotNull Map<String, Literal> options, @NotNull Statement statement) {
        this.options = options;
        this.statement = statement;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(statement);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExplain(this, ctx);
    }

    @NotNull
    public Map<String, Literal> getOptions() {
        return this.options;
    }

    @NotNull
    public Statement getStatement() {
        return this.statement;
    }
}
