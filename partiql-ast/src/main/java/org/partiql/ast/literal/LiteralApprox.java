package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralApprox extends Literal {
    @NotNull
    private final BigDecimal mantissa;

    private final int exponent;

    private LiteralApprox(@NotNull BigDecimal mantissa, int exponent) {
        this.mantissa = mantissa;
        this.exponent = exponent;
    }

    @NotNull
    public static LiteralApprox litApprox(BigDecimal mantissa, int exponent) {
        return new LiteralApprox(mantissa, exponent);
    }

    @NotNull
    public static LiteralApprox litApprox(BigDecimal value) {
        return new LiteralApprox(value, 0);
    }

    @NotNull
    public static LiteralApprox litApprox(float value) {
        return litApprox(BigDecimal.valueOf(value));
    }

    @NotNull
    public static LiteralApprox litApprox(double value) {
        return litApprox(BigDecimal.valueOf(value));
    }

    public double getDouble() {
        return mantissa.scaleByPowerOfTen(exponent).doubleValue();
    }

    @NotNull
    @Override
    public String getText() {
        return mantissa + "E" + exponent;
    }
}
