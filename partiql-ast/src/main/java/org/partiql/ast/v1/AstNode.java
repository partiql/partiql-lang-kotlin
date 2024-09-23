package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Random;

/**
 * TODO docs, equals, hashcode
 */
public abstract class AstNode {
    @NotNull
    public String tag = "Ast-" + String.format("%06x", new Random().nextInt());

    @NotNull
    public abstract Collection<AstNode> children();

    public abstract <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx);
}
