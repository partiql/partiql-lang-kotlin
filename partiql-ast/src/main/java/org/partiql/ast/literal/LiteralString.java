package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralString extends Literal {
    @NotNull
    public String value;

    private LiteralString(@NotNull String value) {
        this.value = value;
    }

    @NotNull
    public static LiteralString litString(@NotNull String value) {
        return new LiteralString(value);
    }

    @NotNull
    @Override
    public String getText() {
        return String.format("'%s'", value);
    }
}
