package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 */
class NullValue implements PQLValue {

    @NotNull
    private final PartiQLValueType _type;

    NullValue() {
        this._type = PartiQLValueType.NULL;
    }

    NullValue(@NotNull PartiQLValueType type) {
        this._type = type;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }

    @Override
    public boolean getBoolean() {
        if (_type == PartiQLValueType.BOOL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public short getShort() {
        if (_type == PartiQLValueType.INT16) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int getInt() {
        if (_type == PartiQLValueType.INT32) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getLong() {
        if (_type == PartiQLValueType.INT64) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigInteger getBigInteger() {
        if (_type == PartiQLValueType.INT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigDecimal getBigDecimal() {
        if (_type == PartiQLValueType.DECIMAL || _type == PartiQLValueType.DECIMAL_ARBITRARY) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public byte getByte() {
        if (_type == PartiQLValueType.BYTE || _type == PartiQLValueType.INT8) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        if (_type == PartiQLValueType.BINARY || _type == PartiQLValueType.BLOB || _type == PartiQLValueType.CLOB) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Date getDate() {
        if (_type == PartiQLValueType.DATE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public double getDouble() {
        if (_type == PartiQLValueType.FLOAT64) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public float getFloat() {
        if (_type == PartiQLValueType.FLOAT32) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<PQLValue> iterator() {
        if (_type == PartiQLValueType.BAG || _type == PartiQLValueType.LIST || _type == PartiQLValueType.SEXP) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<StructField> getFields() {
        if (_type == PartiQLValueType.STRUCT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getString() {
        if (_type == PartiQLValueType.STRING || _type == PartiQLValueType.CHAR || _type == PartiQLValueType.SYMBOL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Time getTime() {
        if (_type == PartiQLValueType.TIME) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Timestamp getTimestamp() {
        if (_type == PartiQLValueType.TIMESTAMP) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getInterval() {
        if (_type == PartiQLValueType.INTERVAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
