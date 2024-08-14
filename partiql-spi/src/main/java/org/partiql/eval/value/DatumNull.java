package org.partiql.eval.value;

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

    @NotNull
    private final PType.Kind _kind;

    DatumNull() {
        this._type = PType.unknown();
        this._kind = _type.getKind();
    }

    DatumNull(@NotNull PType type) {
        this._type = type;
        this._kind = type.getKind();
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
        if (_kind == PType.Kind.BOOL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public short getShort() {
        if (_kind == PType.Kind.SMALLINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int getInt() {
        if (_kind == PType.Kind.INTEGER) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getLong() {
        if (_kind == PType.Kind.BIGINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigInteger getBigInteger() {
        if (_kind == PType.Kind.NUMERIC) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public BigDecimal getBigDecimal() {
        if (_kind == PType.Kind.DECIMAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public byte getByte() {
        if (_kind == PType.Kind.TINYINT) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        if (_kind == PType.Kind.BLOB || _kind == PType.Kind.CLOB) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Date getDate() {
        if (_kind == PType.Kind.DATE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public double getDouble() {
        if (_kind == PType.Kind.DOUBLE) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public float getFloat() {
        if (_kind == PType.Kind.REAL) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Datum> iterator() {
        if (_kind == PType.Kind.BAG || _kind == PType.Kind.ARRAY) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Iterator<Field> getFields() {
        if (_kind == PType.Kind.STRUCT || _kind == PType.Kind.ROW) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public String getString() {
        if (_kind == PType.Kind.STRING || _kind == PType.Kind.CHAR) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Time getTime() {
        if (_kind == PType.Kind.TIMEZ || _kind == PType.Kind.TIME) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public Timestamp getTimestamp() {
        if (_kind == PType.Kind.TIMESTAMPZ || _kind == PType.Kind.TIMESTAMP) {
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
