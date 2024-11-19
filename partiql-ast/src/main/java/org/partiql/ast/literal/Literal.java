package org.partiql.ast.literal;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
public abstract class Literal {
    @NotNull
    public final String text;

    protected Literal(@NotNull String _text) {
        this.text = _text;
    }
}
