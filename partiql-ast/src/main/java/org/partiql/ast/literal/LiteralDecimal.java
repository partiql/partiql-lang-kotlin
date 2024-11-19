package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * TODO DOCS
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralDecimal extends Literal {
    @NotNull
    public BigDecimal value;

    public LiteralDecimal(@NotNull BigDecimal value) {
        super(value.toString());
        this.value = value;
    }
}
