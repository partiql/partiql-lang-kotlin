package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralInt extends Literal {
    public int value;

    public LiteralInt(int value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String getText() {
        return Integer.toString(value);
    }
}
