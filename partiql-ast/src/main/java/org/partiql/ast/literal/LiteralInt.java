package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
class LiteralInt extends Literal {
    @NotNull
    String value;

    LiteralInt(@NotNull String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(value);
    }

    @NotNull
    @Override
    public String numberValue() {
        return value;
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.NUM_INT();
    }
}
