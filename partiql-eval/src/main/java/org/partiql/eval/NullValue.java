package org.partiql.eval;

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
    public boolean getBoolValue() {
        if (_type == PartiQLValueType.BOOL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public byte getInt8Value() {
        if (_type == PartiQLValueType.INT8) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public short getInt16Value() {
        if (_type == PartiQLValueType.INT16) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int getInt32Value() {
        if (_type == PartiQLValueType.INT32) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getInt64Value() {
        if (_type == PartiQLValueType.INT64) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigInteger getIntValue() {
        if (_type == PartiQLValueType.INT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigDecimal getDecimalValue() {
        if (_type == PartiQLValueType.DECIMAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigDecimal getDecimalArbitraryValue() {
        if (_type == PartiQLValueType.DECIMAL_ARBITRARY) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public byte getByteValue() {
        if (_type == PartiQLValueType.BYTE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getBinaryValue() {
        if (_type == PartiQLValueType.BINARY) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getBlobValue() {
        if (_type == PartiQLValueType.BLOB) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getClobValue() {
        if (_type == PartiQLValueType.CLOB) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Date getDateValue() {
        if (_type == PartiQLValueType.DATE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public double getFloat64Value() {
        if (_type == PartiQLValueType.FLOAT64) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public float getFloat32Value() {
        if (_type == PartiQLValueType.FLOAT32) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<PQLValue> getBagValues() {
        if (_type == PartiQLValueType.BAG) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<PQLValue> getListValues() {
        if (_type == PartiQLValueType.LIST) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<PQLValue> getSexpValues() {
        if (_type == PartiQLValueType.SEXP) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<StructField> getStructFields() {
        if (_type == PartiQLValueType.STRUCT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getCharValue() {
        if (_type == PartiQLValueType.CHAR) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getStringValue() {
        if (_type == PartiQLValueType.STRING) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getSymbolValue() {
        if (_type == PartiQLValueType.SYMBOL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Time getTimeValue() {
        if (_type == PartiQLValueType.TIME) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Timestamp getTimestampValue() {
        if (_type == PartiQLValueType.TIMESTAMP) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getIntervalValue() {
        if (_type == PartiQLValueType.INTERVAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
