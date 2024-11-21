package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralInteger extends Literal {
    private final long value;

    private LiteralInteger(long value) {
        this.value = value;
    }

    @NotNull
    public static LiteralInteger litInt(long value) {
        return new LiteralInteger(value);
    }

    @NotNull
    public static LiteralInteger litInt(int value) {
        return new LiteralInteger(value);
    }

    public long getInteger() {
        return value;
    }

    @NotNull
    @Override
    public String getText() {
        return Long.toString(value);
    }
}
