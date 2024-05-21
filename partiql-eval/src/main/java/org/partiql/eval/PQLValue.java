package org.partiql.eval;

import kotlin.NotImplementedError;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQL;
import org.partiql.value.PartiQLValue;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;

/**
 * This is a representation of a value in PartiQL's type system. The intention of this modeling is to provide a layer of
 * indirection between PartiQL's type semantics and Java's type semantics.
 *
 * <p></p>
 * INTERNAL NOTES:
 * <p></p>
 * This is intended to completely replace {@link org.partiql.value.PartiQLValue} in the future. As it stands, this
 * implementation will initially be used solely for the evaluator. However, the scope of this can be expanded by copying
 * and pasting its contents to completely replace {@link org.partiql.value.PartiQLValue}.
 * <p></p>
 * There are some pre-requisites to actually replacing {@link PartiQLValue} including, but not limited to, ...:
 * - The comparator for ordering and aggregations
 * - Adding support for annotations
 */
public interface PQLValue {

    /**
     * Determines whether the current value is a null value of any type (for example, null or null.int). It should be
     * called before calling getters that return value types (int, long, boolean, double).
     */
    boolean isNull();

    /**
     * @return the type of the data at the cursor.
     */
    @NotNull
    PartiQLValueType getType();

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#STRING}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default String getStringValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#CHAR}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default String getCharValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#SYMBOL}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default String getSymbolValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#BOOL}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default boolean getBoolValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#BINARY}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default byte[] getBinaryValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#BLOB}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default byte[] getBlobValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#CLOB}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default byte[] getClobValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#BYTE}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default byte getByteValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#DATE}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Date getDateValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#TIME}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Time getTimeValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#TIMESTAMP}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Timestamp getTimestampValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INTERVAL}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @Deprecated
    default long getIntervalValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INT8}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default byte getInt8Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INT16}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default short getInt16Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INT32}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default int getInt32Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INT64}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default long getInt64Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#INT}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigInteger getIntValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#FLOAT32}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default float getFloat32Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#FLOAT64}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default double getFloat64Value() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#DECIMAL}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigDecimal getDecimalValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#DECIMAL_ARBITRARY}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigDecimal getDecimalArbitraryValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#BAG}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<PQLValue> getBagValues() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#LIST}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<PQLValue> getListValues() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#STRUCT}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<StructField> getStructFields() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PartiQLValueType#SEXP}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PartiQLValueType#INT}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<PQLValue> getSexpValues() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a {@link PQLValue} into a {@link PartiQLValue}.
     *
     * @return the equivalent {@link PartiQLValue}
     * @deprecated this is an experimental API and is designed for use by the internal PartiQL library. This may
     * be modified/removed at any time.
     */
    @NotNull
    @Deprecated
    default PartiQLValue toPartiQLValue() {
        PartiQLValueType type = this.getType();
        switch (type) {
            case BOOL:
                return this.isNull() ? PartiQL.boolValue(null) : PartiQL.boolValue(this.getBoolValue());
            case INT8:
                return this.isNull() ? PartiQL.int8Value(null) : PartiQL.int8Value(this.getInt8Value());
            case INT16:
                return this.isNull() ? PartiQL.int16Value(null) : PartiQL.int16Value(this.getInt16Value());
            case INT32:
                return this.isNull() ? PartiQL.int32Value(null) : PartiQL.int32Value(this.getInt32Value());
            case INT64:
                return this.isNull() ? PartiQL.int64Value(null) : PartiQL.int64Value(this.getInt64Value());
            case INT:
                return this.isNull() ? PartiQL.intValue(null) : PartiQL.intValue(this.getIntValue());
            case DECIMAL:
                return this.isNull() ? PartiQL.decimalValue(null) : PartiQL.decimalValue(this.getDecimalValue());
            case DECIMAL_ARBITRARY:
                return this.isNull() ? PartiQL.decimalValue(null) : PartiQL.decimalValue(this.getDecimalArbitraryValue());
            case FLOAT32:
                return this.isNull() ? PartiQL.float32Value(null) : PartiQL.float32Value(this.getFloat32Value());
            case FLOAT64:
                return this.isNull() ? PartiQL.float64Value(null) : PartiQL.float64Value(this.getFloat64Value());
            case CHAR:
                return this.isNull() ? PartiQL.charValue(null) : PartiQL.charValue(this.getCharValue().charAt(0));
            case STRING:
                return this.isNull() ? PartiQL.stringValue(null) : PartiQL.stringValue(this.getStringValue());
            case SYMBOL:
                return this.isNull() ? PartiQL.symbolValue(null) : PartiQL.symbolValue(this.getSymbolValue());
            case BINARY:
                return this.isNull() ? PartiQL.binaryValue(null) : PartiQL.binaryValue(BitSet.valueOf(this.getBinaryValue()));
            case BYTE:
                return this.isNull() ? PartiQL.byteValue(null) : PartiQL.byteValue(this.getByteValue());
            case BLOB:
                return this.isNull() ? PartiQL.blobValue(null) : PartiQL.blobValue(this.getBlobValue());
            case CLOB:
                return this.isNull() ? PartiQL.clobValue(null) : PartiQL.clobValue(this.getClobValue());
            case DATE:
                return this.isNull() ? PartiQL.dateValue(null) : PartiQL.dateValue(this.getDateValue());
            case TIME:
                return this.isNull() ? PartiQL.timeValue(null) : PartiQL.timeValue(this.getTimeValue());
            case TIMESTAMP:
                return this.isNull() ? PartiQL.timestampValue(null) : PartiQL.timestampValue(this.getTimestampValue());
            case INTERVAL:
                return this.isNull() ? PartiQL.intervalValue(null) : PartiQL.intervalValue(this.getIntervalValue());
            case BAG:
                return this.isNull() ? PartiQL.bagValue((Iterable<? extends PartiQLValue>) null) : PartiQL.bagValue(
                        new IterableFromIteratorSupplier<>(() -> new PQLToPartiQLIterator(this.getBagValues()))
                );
            case LIST:
                return this.isNull() ? PartiQL.listValue((Iterable<? extends PartiQLValue>) null) : PartiQL.listValue(
                        new IterableFromIteratorSupplier<>(() -> new PQLToPartiQLIterator(this.getListValues()))
                );
            case SEXP:
                return this.isNull() ? PartiQL.sexpValue((Iterable<? extends PartiQLValue>) null) : PartiQL.sexpValue(
                        new IterableFromIteratorSupplier<>(() -> new PQLToPartiQLIterator(this.getSexpValues()))
                );
            case STRUCT:
                return this.isNull() ? PartiQL.structValue((Iterable<? extends Pair<String, ? extends PartiQLValue>>) null) : PartiQL.structValue(
                        new IterableFromIteratorSupplier<>(() -> {
                            Iterator<StructField> _fields = this.getStructFields();
                            return new Iterator<Pair<String, PartiQLValue>>() {
                                @Override
                                public boolean hasNext() {
                                    return _fields.hasNext();
                                }

                                @Override
                                public Pair<String, PartiQLValue> next() {
                                    StructField field = _fields.next();
                                    return new Pair<>(field.getName(), field.getValue().toPartiQLValue());
                                }
                            };
                        })
                );
            case NULL:
                return PartiQL.nullValue();
            case MISSING:
                return PartiQL.missingValue();
            case ANY:
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Converts a {@link PartiQLValue} into {@link PQLValue}.
     *
     * @return the equivalent {@link PQLValue}
     */
    @NotNull
    @Deprecated
    static PQLValue of(PartiQLValue value) {
        PartiQLValueType type = value.getType();
        if (value.isNull()) {
            return new NullValue(type);
        }
        switch (type) {
            case MISSING:
                return new MissingValue();
            case NULL:
                return new NullValue();
            case INT8:
                org.partiql.value.Int8Value int8Value = (org.partiql.value.Int8Value) value;
                return new Int8Value(Objects.requireNonNull(int8Value.getValue()));
            case STRUCT:
                @SuppressWarnings("unchecked")
                org.partiql.value.StructValue<PartiQLValue> STRUCTValue = (org.partiql.value.StructValue<PartiQLValue>) value;
                return new StructValue(new StructFieldIterableWrapper(Objects.requireNonNull(STRUCTValue.getEntries())));
            case STRING:
                org.partiql.value.StringValue STRINGValue = (org.partiql.value.StringValue) value;
                return new StringValue(Objects.requireNonNull(STRINGValue.getValue()));
            case INT64:
                org.partiql.value.Int64Value INT64Value = (org.partiql.value.Int64Value) value;
                return new Int64Value(Objects.requireNonNull(INT64Value.getValue()));
            case INT32:
                org.partiql.value.Int32Value INT32Value = (org.partiql.value.Int32Value) value;
                return new Int32Value(Objects.requireNonNull(INT32Value.getValue()));
            case INT16:
                org.partiql.value.Int16Value INT16Value = (org.partiql.value.Int16Value) value;
                return new Int16Value(Objects.requireNonNull(INT16Value.getValue()));
            case SEXP:
                @SuppressWarnings("unchecked")
                org.partiql.value.SexpValue<PartiQLValue> sexpValue = (org.partiql.value.SexpValue<PartiQLValue>) value;
                return new SexpValue(new PartiQLValueIterableWrapper(Objects.requireNonNull(sexpValue)));
            case LIST:
                @SuppressWarnings("unchecked")
                org.partiql.value.ListValue<PartiQLValue> LISTValue = (org.partiql.value.ListValue<PartiQLValue>) value;
                return new ListValue(new PartiQLValueIterableWrapper(Objects.requireNonNull(LISTValue)));
            case BOOL:
                org.partiql.value.BoolValue BOOLValue = (org.partiql.value.BoolValue) value;
                return new BoolValue(Objects.requireNonNull(BOOLValue.getValue()));
            case INT:
                org.partiql.value.IntValue INTValue = (org.partiql.value.IntValue) value;
                return new IntValue(Objects.requireNonNull(INTValue.getValue()));
            case BAG:
                @SuppressWarnings("unchecked")
                org.partiql.value.BagValue<PartiQLValue> BAGValue = (org.partiql.value.BagValue<PartiQLValue>) value;
                return new BagValue(new PartiQLValueIterableWrapper(Objects.requireNonNull(BAGValue)));
            case BINARY:
                org.partiql.value.BinaryValue BINARYValue = (org.partiql.value.BinaryValue) value;
                return new BinaryValue(Objects.requireNonNull(BINARYValue.getValue().toByteArray()));
            case DATE:
                org.partiql.value.DateValue DATEValue = (org.partiql.value.DateValue) value;
                return new DateValue(Objects.requireNonNull(DATEValue.getValue()));
            case INTERVAL:
                org.partiql.value.IntervalValue INTERVALValue = (org.partiql.value.IntervalValue) value;
                return new IntervalValue(Objects.requireNonNull(INTERVALValue.getValue()));
            case TIMESTAMP:
                org.partiql.value.TimestampValue TIMESTAMPValue = (org.partiql.value.TimestampValue) value;
                return new TimestampValue(Objects.requireNonNull(TIMESTAMPValue.getValue()));
            case TIME:
                org.partiql.value.TimeValue TIMEValue = (org.partiql.value.TimeValue) value;
                return new TimeValue(Objects.requireNonNull(TIMEValue.getValue()));
            case FLOAT32:
                org.partiql.value.Float32Value FLOAT32Value = (org.partiql.value.Float32Value) value;
                return new Float32Value(Objects.requireNonNull(FLOAT32Value.getValue()));
            case FLOAT64:
                org.partiql.value.Float64Value FLOAT64Value = (org.partiql.value.Float64Value) value;
                return new Float64Value(Objects.requireNonNull(FLOAT64Value.getValue()));
            case DECIMAL:
                org.partiql.value.DecimalValue DECIMALValue = (org.partiql.value.DecimalValue) value;
                return new DecimalValue(Objects.requireNonNull(DECIMALValue.getValue()));
            case CHAR:
                org.partiql.value.CharValue CHARValue = (org.partiql.value.CharValue) value;
                return new CharValue(Objects.requireNonNull(Objects.requireNonNull(CHARValue.getValue()).toString()));
            case SYMBOL:
                org.partiql.value.SymbolValue SYMBOLValue = (org.partiql.value.SymbolValue) value;
                return new SymbolValue(Objects.requireNonNull(SYMBOLValue.getValue()));
            case CLOB:
                org.partiql.value.ClobValue CLOBValue = (org.partiql.value.ClobValue) value;
                return new ClobValue(Objects.requireNonNull(CLOBValue.getValue()));
            case BLOB:
                org.partiql.value.BlobValue BLOBValue = (org.partiql.value.BlobValue) value;
                return new BlobValue(Objects.requireNonNull(BLOBValue.getValue()));
            case BYTE:
                org.partiql.value.ByteValue BYTEValue = (org.partiql.value.ByteValue) value;
                return new ByteValue(Objects.requireNonNull(BYTEValue.getValue()));
            case DECIMAL_ARBITRARY:
                org.partiql.value.DecimalValue DECIMAL_ARBITRARYValue = (org.partiql.value.DecimalValue) value;
                return new DecimalArbitraryValue(Objects.requireNonNull(DECIMAL_ARBITRARYValue.getValue()));
            case ANY:
            default:
                throw new NotImplementedError();
        }
    }

    @NotNull
    static PQLValue nullValue() {
        return new NullValue();
    }

    @NotNull
    static PQLValue missingValue() {
        return new MissingValue();
    }

    @NotNull
    static PQLValue nullValue(@NotNull PartiQLValueType type) {
        return new NullValue(type);
    }

    @NotNull
    static PQLValue bagValue(@NotNull Iterable<PQLValue> values) {
        return new BagValue(values);
    }

    @NotNull
    static PQLValue int64Value(long value) {
        return new Int64Value(value);
    }

    @NotNull
    static PQLValue int32Value(int value) {
        return new Int32Value(value);
    }

    @NotNull
    static PQLValue boolValue(boolean value) {
        return new BoolValue(value);
    }

    @NotNull
    static PQLValue sexpValue(@NotNull Iterable<PQLValue> values) {
        return new SexpValue(values);
    }

    @NotNull
    static PQLValue listValue(@NotNull Iterable<PQLValue> values) {
        return new ListValue(values);
    }

    @NotNull
    static PQLValue structValue(@NotNull Iterable<StructField> values) {
        return new StructValue(values);
    }

    @NotNull
    static PQLValue stringValue(@NotNull String value) {
        return new StringValue(value);
    }
}
