package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigDecimal;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PartiQLValueType#DECIMAL},
 * {@link PartiQLValueType#DECIMAL_ARBITRARY}
 */
class DatumDecimal implements Datum {

    @NotNull
    private final BigDecimal _value;

    @NotNull
    private final PartiQLValueType _type;

    DatumDecimal(@NotNull BigDecimal value, @NotNull PartiQLValueType type) {
        assert(type == PartiQLValueType.DECIMAL || type == PartiQLValueType.DECIMAL_ARBITRARY);
        _value = value;
        _type = type;
    }

    @Override
    @NotNull
    public BigDecimal getBigDecimal() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}
