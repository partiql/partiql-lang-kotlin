package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
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
    private final PType _type;

    DatumDecimal(@NotNull BigDecimal value, @NotNull PType type) {
        assert(type.getKind() == PType.Kind.DECIMAL || type.getKind() == PType.Kind.DECIMAL_ARBITRARY);
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
    public PType getType() {
        return _type;
    }
}
