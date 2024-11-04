package org.partiql.ast;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class AstEnum extends AstNode {
    public abstract int code();

    @NotNull
    public abstract String name();
}
