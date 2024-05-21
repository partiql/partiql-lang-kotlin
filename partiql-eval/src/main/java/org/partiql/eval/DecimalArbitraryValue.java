package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigDecimal;

/**
 * This shall always be package-private (internal).
 */
class DecimalArbitraryValue implements PQLValue {

    @NotNull
    final BigDecimal _value;

    DecimalArbitraryValue(@NotNull BigDecimal value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public BigDecimal getDecimalArbitraryValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.DECIMAL_ARBITRARY;
    }
}
