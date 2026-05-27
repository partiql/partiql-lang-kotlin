package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * A wrapper around a {@link Datum} that provides value-based hashing and equality
 * suitable for use as a key in a MAP.
 * <p>
 * This shall always be package-private (internal).
 */
class DatumKey {

    private static final Comparator<Datum> COMPARATOR = Datum.comparator(true);

    @NotNull
    private final Datum _datum;

    public DatumKey(@NotNull Datum datum) {
        _datum = datum;
    }

    @NotNull
    public Datum getDatum() {
        return _datum;
    }

    @Override
    public int hashCode() {
        if (_datum.isNull()) {
            throw new IllegalStateException("NULL is not allowed as a MAP key");
        }
        if (_datum.isMissing()) {
            throw new IllegalStateException("MISSING is not allowed as a MAP key");
        }
        int code = _datum.getType().code();
        switch (code) {
            // Numeric types — normalize to BigDecimal for cross-type equality
            case PType.TINYINT:
                return BigDecimal.valueOf(_datum.getByte()).stripTrailingZeros().hashCode();
            case PType.SMALLINT:
                return BigDecimal.valueOf(_datum.getShort()).stripTrailingZeros().hashCode();
            case PType.INTEGER:
                return BigDecimal.valueOf(_datum.getInt()).stripTrailingZeros().hashCode();
            case PType.BIGINT:
                return BigDecimal.valueOf(_datum.getLong()).stripTrailingZeros().hashCode();
            case PType.NUMERIC:
            case PType.DECIMAL:
                return _datum.getBigDecimal().stripTrailingZeros().hashCode();
            case PType.REAL: {
                float f = _datum.getFloat();
                if (Float.isNaN(f) || Float.isInfinite(f)) {
                    throw new IllegalArgumentException("NaN and Infinity are not allowed as MAP keys");
                }
                return BigDecimal.valueOf(f).stripTrailingZeros().hashCode();
            }
            case PType.DOUBLE: {
                double d = _datum.getDouble();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new IllegalArgumentException("NaN and Infinity are not allowed as MAP keys");
                }
                return BigDecimal.valueOf(d).stripTrailingZeros().hashCode();
            }
            // Text types
            case PType.STRING:
            case PType.CHAR:
            case PType.VARCHAR:
                return _datum.getString().hashCode();
            // Boolean
            case PType.BOOL:
                return Boolean.hashCode(_datum.getBoolean());
            // Date/time types — normalize to offset types at UTC for cross-type equality
            case PType.DATE:
                return _datum.getLocalDate().atTime(java.time.LocalTime.MIN).atOffset(java.time.ZoneOffset.UTC).hashCode();
            case PType.TIME:
                return _datum.getLocalTime().atOffset(java.time.ZoneOffset.UTC).hashCode();
            case PType.TIMEZ:
                return _datum.getOffsetTime().withOffsetSameInstant(java.time.ZoneOffset.UTC).hashCode();
            case PType.TIMESTAMP:
                return _datum.getLocalDateTime().atOffset(java.time.ZoneOffset.UTC).hashCode();
            case PType.TIMESTAMPZ:
                return _datum.getOffsetDateTime().withOffsetSameInstant(java.time.ZoneOffset.UTC).hashCode();
            // TODO: Support collection types (ARRAY, BAG, STRUCT, ROW, MAP) as MAP keys
            //  once well-defined equality semantics and hashing for these types are established.
            default:
                throw new UnsupportedOperationException("Type code " + code + " is not supported as a MAP key");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DatumKey)) return false;
        DatumKey other = (DatumKey) obj;
        return COMPARATOR.compare(_datum, other._datum) == 0;
    }

    @Override
    public String toString() {
        return "DatumKey{" + _datum + "}";
    }
}
