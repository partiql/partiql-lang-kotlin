package org.partiql.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * TODO docs, equals, hashcode
 * TODO support source location -- https://github.com/partiql/partiql-lang-kotlin/issues/1608
 */
public abstract class AstNode {
    @NotNull
    public String tag = "Ast-" + String.format("%06x", new Random().nextInt());

    @NotNull
    public abstract List<AstNode> getChildren();

    public abstract <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx);
}
