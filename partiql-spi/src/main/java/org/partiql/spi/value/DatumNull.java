package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
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

    @NotNull
    @Override
    public Date getDate() {
        if (_type.code() == PType.DATE) {
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
    public Time getTime() {
        if (_type.code() == PType.TIMEZ || _type.code() == PType.TIME) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Timestamp getTimestamp() {
        if (_type.code() == PType.TIMESTAMPZ || _type.code() == PType.TIMESTAMP) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getInterval() {
        throw new UnsupportedOperationException();
    }
}
