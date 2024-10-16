package org.partiql.ast.v1;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class Explain extends Statement {
    // TODO get rid of PartiQLValue once https://github.com/partiql/partiql-lang-kotlin/issues/1589 is resolved
    @NotNull
    public final Map<String, PartiQLValue> options;

    @NotNull
    public final Statement statement;

    public Explain(@NotNull Map<String, PartiQLValue> options, @NotNull Statement statement) {
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
