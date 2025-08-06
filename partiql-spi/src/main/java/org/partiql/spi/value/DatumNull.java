package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 */
class DatumNull implements Datum {

    @NotNull
    private final PType _type;

    DatumNull() {
        this._type = PType.unknown();
    }

    DatumNull(@NotNull PType type) {
        this._type = type;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public boolean getBoolean() {
        if (_type.code() == PType.BOOL) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getBoolean");
        }
    }

    @Override
    public short getShort() {
        if (_type.code() == PType.SMALLINT) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getShort");
        }
    }

    @Override
    public int getInt() {
        if (_type.code() == PType.INTEGER) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getInt");
        }
    }

    @Override
    public long getLong() {
        if (_type.code() == PType.BIGINT) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getLong");
        }
    }

    @NotNull
    @Override
    public BigDecimal getBigDecimal() {
        if (_type.code() == PType.DECIMAL || _type.code() == PType.NUMERIC) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getBigDecimal");
        }
    }

    @Override
    public byte getByte() {
        if (_type.code() == PType.TINYINT) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getByte");
        }
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        if (_type.code() == PType.BLOB || _type.code() == PType.CLOB) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getBytes");
        }
    }

    @Override
    public double getDouble() {
        if (_type.code() == PType.DOUBLE) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getDouble");
        }
    }

    @Override
    public float getFloat() {
        if (_type.code() == PType.REAL) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getFloat");
        }
    }

    @NotNull
    @Override
    public Iterator<Datum> iterator() {
        if (_type.code() == PType.BAG || _type.code() == PType.ARRAY) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "iterator");
        }
    }

    @NotNull
    @Override
    public Iterator<Field> getFields() {
        if (_type.code() == PType.STRUCT || _type.code() == PType.ROW) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getFields");
        }
    }

    @NotNull
    @Override
    public String getString() {
        if (_type.code() == PType.STRING || _type.code() == PType.CHAR) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getString");
        }
    }

    @NotNull
    @Override
    public LocalDate getLocalDate() {
        switch (_type.code()) {
            case PType.DATE:
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ:
                throw new NullPointerException();
            default:
                throw new InvalidOperationException(getType(), "getLocalDate");
        }
    }

    @NotNull
    @Override
    public LocalTime getLocalTime() {
        switch (_type.code()) {
            case PType.TIME:
            case PType.TIMEZ:
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ:
                throw new NullPointerException();
            default:
                throw new InvalidOperationException(getType(), "getLocalTime");
        }
    }

    @NotNull
    @Override
    public OffsetTime getOffsetTime() {
        switch (_type.code()) {
            case PType.TIMEZ:
            case PType.TIMESTAMPZ:
                throw new NullPointerException();
            default:
                throw new InvalidOperationException(getType(), "getOffsetTime");
        }
    }

    @NotNull
    @Override
    public LocalDateTime getLocalDateTime() {
        switch (_type.code()) {
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ:
                throw new NullPointerException();
            default:
                throw new InvalidOperationException(getType(), "getLocalDateTime");
        }
    }

    @NotNull
    @Override
    public OffsetDateTime getOffsetDateTime() {
        if (_type.code() == PType.TIMESTAMPZ) {
            throw new NullPointerException();
        } else {
            throw new InvalidOperationException(getType(), "getOffsetDateTime");
        }
    }

    @Override
    public String toString() {
        return "DatumNull{" +
                "_type=" + _type +
                '}';
    }
}
