package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
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
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public short getShort() {
        if (_type.code() == PType.SMALLINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int getInt() {
        if (_type.code() == PType.INTEGER) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getLong() {
        if (_type.code() == PType.BIGINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigInteger getBigInteger() {
        if (_type.code() == PType.NUMERIC) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigDecimal getBigDecimal() {
        if (_type.code() == PType.DECIMAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public byte getByte() {
        if (_type.code() == PType.TINYINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        if (_type.code() == PType.BLOB || _type.code() == PType.CLOB) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public double getDouble() {
        if (_type.code() == PType.DOUBLE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public float getFloat() {
        if (_type.code() == PType.REAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<Datum> iterator() {
        if (_type.code() == PType.BAG || _type.code() == PType.ARRAY) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<Field> getFields() {
        if (_type.code() == PType.STRUCT || _type.code() == PType.ROW) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getString() {
        if (_type.code() == PType.STRING || _type.code() == PType.CHAR) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public OffsetDateTime getOffsetDateTime() {
        if (_type.code() == PType.TIMESTAMPZ) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Duration getDuration() {
        switch (_type.code()) {
            // TODO INTERVAL_DT
            // case INTERVAL_DT:
            //    throw new NullPointerException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Period getPeriod() {
        switch (_type.code()) {
            // TODO INTERVAL_YM
            // case INTERVAL_YM:
            //    throw new NullPointerException();
            default:
                throw new UnsupportedOperationException();
        }
    }
}
