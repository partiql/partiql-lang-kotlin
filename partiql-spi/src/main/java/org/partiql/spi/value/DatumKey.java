package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

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
                return Byte.hashCode(_datum.getByte());
            case PType.SMALLINT:
                return Short.hashCode(_datum.getShort());
            case PType.INTEGER:
                return Integer.hashCode(_datum.getInt());
            case PType.BIGINT:
                return Long.hashCode(_datum.getLong());
            case PType.NUMERIC:
            case PType.DECIMAL:
                return _datum.getBigDecimal().hashCode();
            case PType.REAL:
                return Float.hashCode(_datum.getFloat());
            case PType.DOUBLE:
                return Double.hashCode(_datum.getDouble());
            case PType.STRING:
            case PType.CHAR:
            case PType.VARCHAR:
                return _datum.getString().hashCode();
            case PType.BOOL:
                return Boolean.hashCode(_datum.getBoolean());
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
