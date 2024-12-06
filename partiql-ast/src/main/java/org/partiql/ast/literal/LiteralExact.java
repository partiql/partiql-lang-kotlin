package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * TODO docs
 * Differs from LiteralInt in that it will always include a decimal point.
 */
@EqualsAndHashCode(callSuper = false)
class LiteralExact extends Literal {
    @NotNull
    String value;

    LiteralExact(@NotNull String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String numberValue() {
        return value;
    }

    @NotNull
    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(value, new MathContext(38, RoundingMode.HALF_EVEN));
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.NUM_EXACT();
    }
}
