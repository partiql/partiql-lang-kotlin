package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralBool extends Literal {
    public boolean value;

    private LiteralBool(boolean value) {
        this.value = value;
    }

    @NotNull
    public static LiteralBool litBool(boolean value) {
        return new LiteralBool(value);
    }

    @NotNull
    @Override
    public String getText() {
        return Boolean.toString(value);
    }
}
