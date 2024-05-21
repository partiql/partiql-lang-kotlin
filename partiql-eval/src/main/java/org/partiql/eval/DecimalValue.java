package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigDecimal;

/**
 * This shall always be package-private (internal).
 */
class DecimalValue implements PQLValue {

    @NotNull
    final BigDecimal _value;

    DecimalValue(@NotNull BigDecimal value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public BigDecimal getDecimalValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.DECIMAL;
    }
}
