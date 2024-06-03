package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigDecimal;

/**
 * This shall always be package-private (internal).
 */
class DecimalValue implements PQLValue {

    @NotNull
    private final BigDecimal _value;

    DecimalValue(@NotNull BigDecimal value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public BigDecimal getBigDecimal() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.DECIMAL;
    }
}
