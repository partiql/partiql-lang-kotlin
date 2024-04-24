package org.partiql.value;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

class PartiQLCursorDefault implements PartiQLCursor {

    @NotNull
    private final Stack<Iterator<NamedValue>> iteratorStack;

    @NotNull
    private Iterator<NamedValue> currentIter;

    private NamedValue currentValue;

    PartiQLCursorDefault(PartiQLValue delegate) {
        List<PartiQLValue> wrappedList = new ArrayList<>();
        wrappedList.add(delegate);
        Iterator<NamedValue> topLevelIterator = unnamed(wrappedList.iterator());
        this.iteratorStack = new Stack<>();
        this.iteratorStack.push(topLevelIterator);
        this.currentIter = topLevelIterator;
        this.currentValue = null;
    }

    @Override
    public void close() {
        currentIter = Collections.emptyIterator();
        currentValue = null;
        iteratorStack.empty();
    }

    @Override
    public boolean hasNext() {
        return currentIter.hasNext();
    }

    @NotNull
    @Override
    public PartiQLValueType next() {
        currentValue = currentIter.next();
        return currentValue.value.getType();
    }

    @Override
    public void stepIn() {
        org.partiql.value.PartiQLValue value = currentValue.value;
        PartiQLValueType type = currentValue.value.getType();
        Iterator<NamedValue> children;
        switch (type) {
            case LIST:
                @SuppressWarnings("unchecked")
                ListValue<PartiQLValue> list = (ListValue<PartiQLValue>) value;
                children = unnamed(list.iterator());
                break;
            case BAG:
                @SuppressWarnings("unchecked")
                BagValue<PartiQLValue> bag = (BagValue<PartiQLValue>) value;
                children = unnamed(bag.iterator());
                break;
            case SEXP:
                @SuppressWarnings("unchecked")
                SexpValue<PartiQLValue> sexp = (SexpValue<PartiQLValue>) value;
                children = unnamed(sexp.iterator());
                break;
            case STRUCT:
                @SuppressWarnings("unchecked")
                StructValue<PartiQLValue> struct = (StructValue<PartiQLValue>) value;
                children = named(struct.getEntries());
                break;
            default:
                throw new IllegalStateException();
        }
        iteratorStack.push(children);
        currentValue = null;
        currentIter = iteratorStack.peek();
    }

    @Override
    public void stepOut() {
        iteratorStack.pop();
        currentValue = null;
        currentIter = iteratorStack.peek();
    }

    @Override
    public boolean isNullValue() {
        return currentValue.value.isNull();
    }

    @Override
    public boolean isMissingValue() {
        return currentValue.value.getType() == PartiQLValueType.MISSING;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return currentValue.value.getType();
    }

    @Override
    public String getFieldName() {
        return currentValue.name;
    }

    @NotNull
    @Override
    public String getStringValue() {
        if (currentValue.value.getType() == PartiQLValueType.STRING) {
            return Objects.requireNonNull(((StringValue) currentValue.value).getValue());
        }
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public String getCharValue() {
        if (currentValue.value.getType() == PartiQLValueType.CHAR) {
            return Objects.requireNonNull((Objects.requireNonNull(((CharValue) currentValue.value).getValue()).toString()));
        }
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public String getSymbolValue() {
        if (currentValue.value.getType() == PartiQLValueType.SYMBOL) {
            return Objects.requireNonNull(((SymbolValue) currentValue.value).getValue());
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean getBoolValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.BOOL) {
            BoolValue value = (BoolValue) (currentValue.value);
            return Boolean.TRUE.equals(value.getValue());
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public byte[] getBinaryValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.BINARY) {
            BinaryValue binaryValue = (BinaryValue) (currentValue.value);
            return Objects.requireNonNull(binaryValue.getValue()).toByteArray();
        }
        throw new IllegalStateException();
    }

    @Override
    public byte[] getBlobValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.BLOB) {
            BlobValue blobValue = (BlobValue) (currentValue.value);
            return Objects.requireNonNull(blobValue.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
    public byte[] getClobValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.CLOB) {
            ClobValue clobValue = (ClobValue) (currentValue.value);
            return Objects.requireNonNull(clobValue.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
    public byte getByteValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.BYTE) {
            ByteValue byteValue = (ByteValue) (currentValue.value);
            return Objects.requireNonNull(byteValue.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	@NotNull
	public Date getDateValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.DATE) {
            DateValue value = (DateValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	@NotNull
	public Time getTimeValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.TIME) {
            TimeValue value = (TimeValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	@NotNull
	public Timestamp getTimestampValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.TIMESTAMP) {
            TimestampValue value = (TimestampValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Deprecated
    @Override
	public long getIntervalValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INTERVAL) {
            IntervalValue value = (IntervalValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
    public byte getInt8Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INT8) {
            Int8Value value = (Int8Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	public short getInt16Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INT16) {
            Int16Value value = (Int16Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	public int getInt32Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INT32) {
            Int32Value value = (Int32Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	public long getInt64Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INT64) {
            Int64Value value = (Int64Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	@NotNull
	public BigInteger getIntValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.INT) {
            IntValue value = (IntValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	public float getFloat32Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.FLOAT32) {
            Float32Value value = (Float32Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	public double getFloat64Value() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.FLOAT64) {
            Float64Value value = (Float64Value) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @Override
	@NotNull
	public BigDecimal getDecimalValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.DECIMAL) {
            DecimalValue value = (DecimalValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public BigDecimal getDecimalArbitraryValue() {
        PartiQLValueType type = currentValue.value.getType();
        if (type == PartiQLValueType.DECIMAL_ARBITRARY) {
            DecimalValue value = (DecimalValue) (currentValue.value);
            return Objects.requireNonNull(value.getValue());
        }
        throw new IllegalStateException();
    }

    private NamedIterator named(Iterable<Pair<String, PartiQLValue>> values) {
        return new NamedIterator(values);
    }

    private UnnamedIterator unnamed(Iterator<PartiQLValue> values) {
        return new UnnamedIterator(values);
    }

    private static class UnnamedIterator implements Iterator<NamedValue> {

        @NotNull
        Iterator<PartiQLValue> values;

        UnnamedIterator(@NotNull Iterator<PartiQLValue> values) {
            this.values = values;
        }

        @Override
        public boolean hasNext() {
            return values.hasNext();
        }

        @Override
        public NamedValue next() {
            return new NamedValue(values.next());
        }
    }

    private static class NamedIterator implements Iterator<NamedValue> {

        @NotNull
        Iterator<Pair<String, PartiQLValue>> values;

        NamedIterator(@NotNull Iterable<Pair<String, PartiQLValue>> values) {
            this.values = values.iterator();
        }

        @Override
        public boolean hasNext() {
            return values.hasNext();
        }

        @Override
        public NamedValue next() {
            Pair<String, PartiQLValue> next = values.next();
            return new NamedValue(next.getFirst(), next.getSecond());
        }
    }

    private static class NamedValue {
        public String name;

        @NotNull
        public PartiQLValue value;

        private NamedValue(String name, @NotNull PartiQLValue value) {
            this.name = name;
            this.value = value;
        }

        private NamedValue(@NotNull PartiQLValue value) {
            this.name = null;
            this.value = value;
        }
    }
}
