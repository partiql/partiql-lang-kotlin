package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
class LiteralApprox extends Literal {
    @NotNull
    String value;

    LiteralApprox(@NotNull String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String numberValue() {
        return value;
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.NUM_APPROX();
    }
}
