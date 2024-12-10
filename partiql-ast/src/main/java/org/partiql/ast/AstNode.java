package org.partiql.ast;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TODO docs, equals, hashcode
 * TODO support source location -- https://github.com/partiql/partiql-lang-kotlin/issues/1608
 */
@Setter
@Getter
public abstract class AstNode {
    private int tag = 0;

    @NotNull
    public abstract List<AstNode> getChildren();

    public abstract <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx);
}
