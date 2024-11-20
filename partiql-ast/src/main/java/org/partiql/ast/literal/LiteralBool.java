package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralBool extends Literal {
    public boolean value;

    public LiteralBool(boolean value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String getText() {
        return Boolean.toString(value);
    }
}
