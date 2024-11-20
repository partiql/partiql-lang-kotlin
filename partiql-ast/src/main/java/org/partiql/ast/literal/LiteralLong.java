package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralLong extends Literal {
    public long value;

    public LiteralLong(long value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String getText() {
        return Long.toString(value);
    }
}
