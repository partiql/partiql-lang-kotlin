package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * TODO DOCS
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralExact extends Literal {
    @NotNull
    private final BigDecimal value;

    private LiteralExact(@NotNull BigDecimal value) {
        this.value = value;
    }

    @NotNull
    public static LiteralExact litExact(BigDecimal value) {
        return new LiteralExact(value);
    }

    @NotNull
    public static LiteralExact litExact(BigInteger value) {
        return new LiteralExact(new BigDecimal(value));
    }

    @NotNull
    public BigDecimal getDecimal() {
        return value;
    }

    @NotNull
    @Override
    public String getText() {
        return value.toString();
    }
}
