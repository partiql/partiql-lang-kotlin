package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType#DECIMAL},
 * {@link PType#NUMERIC}
 */
class DatumDecimal implements Datum {

    @NotNull
    private final BigDecimal _value;

    @NotNull
    private final PType _type;

    DatumDecimal(@NotNull BigDecimal value, @NotNull PType type) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Datum)) return false;
        Datum data = (Datum) o;
        return Objects.equals(_type, data.getType()) && Objects.equals(_value, data.getBigDecimal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(_value, _type);
    }

    @Override
    public String toString() {
        return "DatumDecimal{" +
                "_value=" + _value +
                ", _type=" + _type +
                '}';
    }
}
