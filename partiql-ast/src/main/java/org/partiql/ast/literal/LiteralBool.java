package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
class LiteralBool extends Literal {
    private final boolean value;

    LiteralBool(boolean value) {
        this.value = value;
    }

    @Override
    public boolean booleanValue() {
        return value;
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.BOOLEAN();
    }
}
