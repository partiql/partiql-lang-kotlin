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
            case PType.REAL:
                return BigDecimal.valueOf(_datum.getFloat()).stripTrailingZeros().hashCode();
            case PType.DOUBLE:
                return BigDecimal.valueOf(_datum.getDouble()).stripTrailingZeros().hashCode();
            // Text types
            case PType.STRING:
            case PType.CHAR:
            case PType.VARCHAR:
                return _datum.getString().hashCode();
            // Boolean
            case PType.BOOL:
                return Boolean.hashCode(_datum.getBoolean());
            // Date/time types
            case PType.DATE:
                return _datum.getLocalDate().hashCode();
            case PType.TIME:
                return _datum.getLocalTime().hashCode();
            case PType.TIMEZ:
                return _datum.getOffsetTime().hashCode();
            case PType.TIMESTAMP:
                return _datum.getLocalDateTime().hashCode();
            case PType.TIMESTAMPZ:
                return _datum.getOffsetDateTime().hashCode();
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
