package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
class LiteralString extends Literal {
    @NotNull
    String value;

    LiteralString(@NotNull String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String stringValue() {
        return value;
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.STRING();
    }
}
