package org.partiql.spi.value;

import kotlin.NotImplementedError;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.errors.DataException;
import org.partiql.types.PType;
import org.partiql.value.PartiQL;
import org.partiql.value.PartiQLValue;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

import static org.partiql.types.PType.DYNAMIC;
import static org.partiql.types.PType.BOOL;
import static org.partiql.types.PType.TINYINT;
import static org.partiql.types.PType.SMALLINT;
import static org.partiql.types.PType.INTEGER;
import static org.partiql.types.PType.BIGINT;
import static org.partiql.types.PType.NUMERIC;
import static org.partiql.types.PType.DECIMAL;
import static org.partiql.types.PType.REAL;
import static org.partiql.types.PType.DOUBLE;
import static org.partiql.types.PType.CHAR;
import static org.partiql.types.PType.STRING;
import static org.partiql.types.PType.BLOB;
import static org.partiql.types.PType.CLOB;
import static org.partiql.types.PType.DATE;
import static org.partiql.types.PType.TIME;
import static org.partiql.types.PType.TIMEZ;
import static org.partiql.types.PType.TIMESTAMP;
import static org.partiql.types.PType.TIMESTAMPZ;
import static org.partiql.types.PType.ARRAY;
import static org.partiql.types.PType.BAG;
import static org.partiql.types.PType.ROW;
import static org.partiql.types.PType.STRUCT;
import static org.partiql.types.PType.UNKNOWN;

/**
 * This is an EXPERIMENTAL representation of a value in PartiQL's type system. The intention of this modeling is to
 * provide a layer of indirection between PartiQL's type semantics and Java's type semantics.
 * <p>
 * ! EXPERIMENTAL ! This API is experimental and may be removed/modified without prior notice.
 * </p>
 * <p></p>
 * INTERNAL DEVELOPER NOTES:
 * <p></p>
 * This is intended to completely replace {@link org.partiql.value.PartiQLValue} in the future (for evaluation). As it
 * stands, this implementation will initially be used solely for the evaluator. {@link org.partiql.value.PartiQLValue}
 * may be modified to be solely used for the plan representation (DOM).
 * <p></p>
 * Note that this is public, however, it will not be released until it replaces  {@link org.partiql.value.PartiQLValue}
 * for evaluation.
 * <p></p>
 * There are some pre-requisites to actually replacing {@link PartiQLValue} for evaluation including, but not limited to:
 * - The comparator for ordering and aggregations
 * - Equality
 * - Adding support for annotations
 */
public interface Datum extends Iterable<Datum> {

    /**
     * Determines whether the current value is a null value of any type (for example, null or null.int). It should be
     * called before calling getters that return value types (int, long, boolean, double).
     * <p></p>
     * The default implementation returns false.
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Determines whether the current value is a null value of any type (for example, null or null.int). It should be
     * called before calling getters that return value types (int, long, boolean, double).
     * <p></p>
     * The default implementation returns false.
     */
    default boolean isMissing() {
        return false;
    }

    /**
     * @return the type of the data at the cursor.
     */
    @NotNull
    PType getType();

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#STRING},
     * {@link PType#CHAR}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default String getString() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#BOOL}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * </p>
     * @return the underlying value applicable to the types:
     * {@link PType#BLOB},
     * {@link PType#CLOB}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated BINARY doesn't exist in SQL or Ion. This is subject to deletion. BLOB and CLOB are typically represented
     * in a fashion that can support much larger values -- this may be modified at any time.
     */
    @NotNull
    default byte[] getBytes() {
        throw new UnsupportedOperationException();
    }

    /**
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * @return the underlying value applicable to the types:
     * {@link PType#TINYINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated BYTE is not present in SQL or Ion. This is subject to deletion.
     */
    @Deprecated
    default byte getByte() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#DATE}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default org.partiql.value.datetime.Date getDate() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#TIME}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Time getTime() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#TIMESTAMP}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Timestamp getTimestamp() {
        throw new UnsupportedOperationException();
    }

    /**
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * @return the underlying value applicable to the types:
     * TODO
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated This implementation is likely wrong and is not recommended for use.
     */
    @Deprecated
    default long getInterval() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#SMALLINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default short getShort() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#INTEGER}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default int getInt() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#BIGINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default long getLong() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#INTEGER}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigInteger getBigInteger() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#REAL}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default float getFloat() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#DOUBLE}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default double getDouble() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#DECIMAL}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigDecimal getBigDecimal() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the elements of either bags/lists; returns the fields' values if the type is a struct.
     * @throws UnsupportedOperationException if this operation is invoked on a value that is not of the following
     *                                       types: {@link PType#BAG}, {@link PType#ARRAY}, and
     *                                       {@link PType#STRUCT}.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    @Override
    default Iterator<Datum> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying values applicable to the type {@link PType#STRUCT}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<Field> getFields() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PType#STRUCT} and requested field name. This
     * is a case-sensitive lookup.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum get(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PType#STRUCT} and requested field name. This
     * is a case-insensitive lookup.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum getInsensitive(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pack a VARIANT into a byte array with the given charset.
     *
     * @param charset optional charset.
     * @return the variant as an encoded byte[].
     * @throws UnsupportedOperationException if the datum is not a VARIANT.
     */
    default byte[] pack(@Nullable Charset charset) {
        if (charset != null) {
            throw new UnsupportedOperationException("variant does not support encoding to charset: " + charset.name());
        } else {
            throw new UnsupportedOperationException("variant does not support encoding to byte[]");
        }
    }

    /**
     * Lower a VARIANT into a non-VARIANT datum.
     *
     * @return a non-VARIANT datum.
     * @throws UnsupportedOperationException if the datum is not a VARIANT.
     */
    default Datum lower() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a {@link Datum} into a {@link PartiQLValue}.
     *
     * @return the equivalent {@link PartiQLValue}
     * @deprecated this is an experimental API and is designed for use by the internal PartiQL library. This may
     * be modified/removed at any time.
     */
    @NotNull
    @Deprecated
    default PartiQLValue toPartiQLValue() {
        PType type = this.getType();
        if (this.isMissing()) {
            return PartiQL.missingValue();
        }
        switch (type.code()) {
            case BOOL:
                return this.isNull() ? PartiQL.boolValue(null) : PartiQL.boolValue(this.getBoolean());
            case TINYINT:
                return this.isNull() ? PartiQL.int8Value(null) : PartiQL.int8Value(this.getByte());
            case SMALLINT:
                return this.isNull() ? PartiQL.int16Value(null) : PartiQL.int16Value(this.getShort());
            case INTEGER:
                return this.isNull() ? PartiQL.int32Value(null) : PartiQL.int32Value(this.getInt());
            case BIGINT:
                return this.isNull() ? PartiQL.int64Value(null) : PartiQL.int64Value(this.getLong());
            case NUMERIC:
                return this.isNull() ? PartiQL.intValue(null) : PartiQL.intValue(this.getBigInteger());
            case DECIMAL:
                return this.isNull() ? PartiQL.decimalValue(null) : PartiQL.decimalValue(this.getBigDecimal());
            case REAL:
                return this.isNull() ? PartiQL.float32Value(null) : PartiQL.float32Value(this.getFloat());
            case DOUBLE:
                return this.isNull() ? PartiQL.float64Value(null) : PartiQL.float64Value(this.getDouble());
            case CHAR:
                return this.isNull() ? PartiQL.charValue(null) : PartiQL.charValue(this.getString().charAt(0));
            case STRING:
                return this.isNull() ? PartiQL.stringValue(null) : PartiQL.stringValue(this.getString());
            case BLOB:
                return this.isNull() ? PartiQL.blobValue(null) : PartiQL.blobValue(this.getBytes());
            case CLOB:
                return this.isNull() ? PartiQL.clobValue(null) : PartiQL.clobValue(this.getBytes());
            case DATE:
                return this.isNull() ? PartiQL.dateValue(null) : PartiQL.dateValue(this.getDate());
            case TIMEZ:
            case TIME: // TODO
                return this.isNull() ? PartiQL.timeValue(null) : PartiQL.timeValue(this.getTime());
            case TIMESTAMPZ:
            case TIMESTAMP:
                return this.isNull() ? PartiQL.timestampValue(null) : PartiQL.timestampValue(this.getTimestamp());
            case BAG:
                return this.isNull() ? PartiQL.bagValue((Iterable<? extends PartiQLValue>) null) : PartiQL.bagValue(new PQLToPartiQLIterable(this));
            case ARRAY:
                return this.isNull() ? PartiQL.listValue((Iterable<? extends PartiQLValue>) null) : PartiQL.listValue(new PQLToPartiQLIterable(this));
            case STRUCT:
            case ROW:
                return this.isNull() ? PartiQL.structValue((Iterable<? extends Pair<String, ? extends PartiQLValue>>) null) : PartiQL.structValue(new PQLToPartiQLStruct(this));
            case DYNAMIC:
            case UNKNOWN:
                if (this.isNull()) {
                    return PartiQL.nullValue();
                } else if (this.isMissing()) {
                    return PartiQL.missingValue();
                }
            default:
                throw new UnsupportedOperationException("Unsupported datum type: " + type);
        }
    }

    /**
     * Converts a {@link PartiQLValue} into {@link Datum}.
     *
     * @return the equivalent {@link Datum}
     */
    @NotNull
    @Deprecated
    static Datum of(PartiQLValue value) {
        PartiQLValueType type = value.getType();
        if (value.isNull()) {
            return new DatumNull(type.toPType());
        }
        switch (type) {
            case MISSING:
                return new DatumMissing();
            case NULL:
                return new DatumNull();
            case INT8:
                org.partiql.value.Int8Value int8Value = (org.partiql.value.Int8Value) value;
                return new DatumByte(Objects.requireNonNull(int8Value.getValue()), PType.tinyint());
            case STRUCT:
                @SuppressWarnings("unchecked") org.partiql.value.StructValue<PartiQLValue> STRUCTValue = (org.partiql.value.StructValue<PartiQLValue>) value;
                return new DatumStruct(new PartiQLToPQLStruct(Objects.requireNonNull(STRUCTValue)));
            case STRING:
                org.partiql.value.StringValue STRINGValue = (org.partiql.value.StringValue) value;
                return new DatumString(Objects.requireNonNull(STRINGValue.getValue()), PType.string());
            case INT64:
                org.partiql.value.Int64Value INT64Value = (org.partiql.value.Int64Value) value;
                return new DatumLong(Objects.requireNonNull(INT64Value.getValue()));
            case INT32:
                org.partiql.value.Int32Value INT32Value = (org.partiql.value.Int32Value) value;
                return new DatumInt(Objects.requireNonNull(INT32Value.getValue()));
            case INT16:
                org.partiql.value.Int16Value INT16Value = (org.partiql.value.Int16Value) value;
                return new DatumShort(Objects.requireNonNull(INT16Value.getValue()));
            case SEXP:
                throw new IllegalStateException("SEXP is not supported.");
            case LIST:
                @SuppressWarnings("unchecked") org.partiql.value.ListValue<PartiQLValue> LISTValue = (org.partiql.value.ListValue<PartiQLValue>) value;
                return new DatumCollection(new PartiQLToPQLIterable(Objects.requireNonNull(LISTValue)), PType.array());
            case BOOL:
                org.partiql.value.BoolValue BOOLValue = (org.partiql.value.BoolValue) value;
                return new DatumBoolean(Objects.requireNonNull(BOOLValue.getValue()));
            case INT:
                org.partiql.value.IntValue INTValue = (org.partiql.value.IntValue) value;
                return new DatumBigInteger(Objects.requireNonNull(INTValue.getValue()));
            case BAG:
                @SuppressWarnings("unchecked") org.partiql.value.BagValue<PartiQLValue> BAGValue = (org.partiql.value.BagValue<PartiQLValue>) value;
                return new DatumCollection(new PartiQLToPQLIterable(Objects.requireNonNull(BAGValue)), PType.bag());
            case BINARY:
                throw new UnsupportedOperationException();
            case DATE:
                org.partiql.value.DateValue DATEValue = (org.partiql.value.DateValue) value;
                return new DatumDate(Objects.requireNonNull(DATEValue.getValue()));
            case INTERVAL:
                org.partiql.value.IntervalValue INTERVALValue = (org.partiql.value.IntervalValue) value;
                return new DatumInterval(Objects.requireNonNull(INTERVALValue.getValue()));
            case TIMESTAMP:
                org.partiql.value.TimestampValue TIMESTAMPValue = (org.partiql.value.TimestampValue) value;
                return new DatumTimestamp(Objects.requireNonNull(TIMESTAMPValue.getValue()));
            case TIME:
                org.partiql.value.TimeValue TIMEValue = (org.partiql.value.TimeValue) value;
                return new DatumTime(Objects.requireNonNull(TIMEValue.getValue()));
            case FLOAT32:
                org.partiql.value.Float32Value FLOAT32Value = (org.partiql.value.Float32Value) value;
                return new DatumFloat(Objects.requireNonNull(FLOAT32Value.getValue()));
            case FLOAT64:
                org.partiql.value.Float64Value FLOAT64Value = (org.partiql.value.Float64Value) value;
                return new DatumDouble(Objects.requireNonNull(FLOAT64Value.getValue()));
            case DECIMAL:
            case DECIMAL_ARBITRARY:
                org.partiql.value.DecimalValue DECIMALValue = (org.partiql.value.DecimalValue) value;
                BigDecimal d = Objects.requireNonNull(DECIMALValue.getValue());
                return Datum.decimal(d, d.precision(), d.scale());
            case CHAR:
                org.partiql.value.CharValue CHARValue = (org.partiql.value.CharValue) value;
                String charString = Objects.requireNonNull(CHARValue.getValue()).toString();
                return new DatumChars(charString, charString.length());
            case SYMBOL:
                throw new IllegalStateException("SYMBOL not supported.");
            case CLOB:
                org.partiql.value.ClobValue CLOBValue = (org.partiql.value.ClobValue) value;
                return new DatumBytes(Objects.requireNonNull(CLOBValue.getValue()), PType.clob(Integer.MAX_VALUE)); // TODO
            case BLOB:
                org.partiql.value.BlobValue BLOBValue = (org.partiql.value.BlobValue) value;
                return new DatumBytes(Objects.requireNonNull(BLOBValue.getValue()), PType.blob(Integer.MAX_VALUE)); // TODO
            case BYTE:
                throw new UnsupportedOperationException();
            case ANY:
            default:
                throw new NotImplementedError();
        }
    }

    @NotNull
    static Datum nullValue() {
        return new DatumNull();
    }

    @NotNull
    static Datum missing() {
        return new DatumMissing();
    }

    @NotNull
    static Datum nullValue(@NotNull PType type) {
        return new DatumNull(type);
    }

    /**
     * Returns a typed missing value
     * ! EXPERIMENTAL ! This is subject to breaking changes and/or removal without prior notice.
     * @param type the type of the value
     * @return a typed missing value
     * @deprecated this may not be required. This is subject to removal.
     */
    @Deprecated
    @NotNull
    static Datum missing(@NotNull PType type) {
        return new DatumMissing(type);
    }

    @NotNull
    static Datum bool(boolean value) {
        return new DatumBoolean(value);
    }

    // NUMERIC

    @NotNull
    static Datum tinyint(byte value) {
        return new DatumByte(value, PType.tinyint());
    }

    @NotNull
    static Datum smallint(short value) {
        return new DatumShort(value);
    }

    @NotNull
    static Datum integer(int value) {
        return new DatumInt(value);
    }

    @NotNull
    static Datum bigint(long value) {
        return new DatumLong(value);
    }

    @Deprecated
    @NotNull
    static Datum numeric(@NotNull BigInteger value) {
        return new DatumBigInteger(value);
    }

    @NotNull
    static Datum real(float value) {
        return new DatumFloat(value);
    }

    @NotNull
    static Datum doublePrecision(double value) {
        return new DatumDouble(value);
    }

    @NotNull
    @Deprecated
    static Datum decimal(@NotNull BigDecimal value) {
        return new DatumDecimal(value, PType.decimal(38, 0));
    }

    @NotNull
    static Datum decimal(@NotNull BigDecimal value, int precision, int scale) throws DataException {
        BigDecimal d = value.round(new MathContext(precision)).setScale(scale, RoundingMode.HALF_UP);
        if (d.precision() > precision) {
            throw new DataException("Value " + d + " could not fit into decimal with precision " + precision + " and scale " + scale + ".");
        }
        return new DatumDecimal(d, PType.decimal(precision, scale));
    }

    // CHARACTER STRINGS

    @NotNull
    static Datum string(@NotNull String value) {
        return new DatumString(value, PType.string());
    }

    /**
     *
     * @param value the string to place in the varchar
     * @return a varchar value with a default length of 255
     */
    @NotNull
    static Datum varchar(@NotNull String value) {
        return varchar(value, 255);
    }

    /**
     *
     * @param value the string to place in the varchar
     * @return a varchar value
     * TODO: Error or coerce here? Right now coerce, though I think this should likely error.
     */
    @NotNull
    static Datum varchar(@NotNull String value, int length) {
        String newValue;
        if (length <= 0) {
            throw new DataException("VARCHAR of length " + length + " not allowed.");
        }
        if (value.length() < length) {
            newValue = String.format("%-" + length + "." + length + "s", value);
        } else if (value.length() == length) {
            newValue = value;
        } else {
            newValue = value.substring(0, length);
        }
        return new DatumString(newValue, PType.varchar(length));
    }

    /**
     *
     * @param value the string to place in the char
     * @return a char value with a default length of 255
     */
    @NotNull
    static Datum character(@NotNull String value) {
        return character(value, 255);
    }

    /**
     *
     * @param value the string to place in the char
     * @return a char value
     */
    @NotNull
    static Datum character(@NotNull String value, int length) {
        String newValue;
        if (length <= 0) {
            throw new DataException("CHAR of length " + length + " not allowed.");
        }
        if (value.length() < length) {
            newValue = String.format("%-" + length + "." + length + "s", value);
        } else if (value.length() == length) {
            newValue = value;
        } else {
            newValue = value.substring(0, length);
        }
        return new DatumString(newValue, PType.character(length));
    }

    @NotNull
    static Datum clob(@NotNull byte[] value) {
        return clob(value, Integer.MAX_VALUE);
    }

    @NotNull
    static Datum clob(@NotNull byte[] value, int length) {
        return new DatumBytes(value, PType.clob(length));
    }

    // BYTE STRINGS

    @NotNull
    static Datum blob(@NotNull byte[] value) {
        return new DatumBytes(value, PType.blob(Integer.MAX_VALUE));
    }

    @NotNull
    static Datum blob(@NotNull byte[] value, int length) {
        return new DatumBytes(value, PType.blob(length));
    }

    // DATE/TIME

    @NotNull
    static Datum date(@NotNull Date value) {
        return new DatumDate(value);
    }

    @NotNull
    static Datum time(@NotNull Time value) {
        return new DatumTime(value);
    }

    @NotNull
    static Datum timestamp(@NotNull Timestamp value) {
        return new DatumTimestamp(value);
    }

    // COLLECTIONS

    @NotNull
    static Datum bag(@NotNull Iterable<Datum> values) {
        return new DatumCollection(values, PType.bag());
    }

    @NotNull
    static Datum array(@NotNull Iterable<Datum> values) {
        return new DatumCollection(values, PType.array());
    }

    // STRUCTURAL

    @NotNull
    static Datum struct() {
        return struct(Collections.emptyList());
    }

    @NotNull
    static Datum struct(@NotNull Iterable<Field> values) {
        return new DatumStruct(values);
    }

    /**
     * Comparator for PartiQL values.
     * <p>
     * This may be used for the comparison operators, GROUP BY, ORDER BY, and DISTINCT. The conventional use
     * of {@link java.util.HashMap}, {@link java.util.HashSet}, {@link Object#hashCode()}, and
     * {@link Object#equals(Object)} will not work outright with Datum to implement the before-mentioned operations due
     * to requirements by the PartiQL and SQL Specifications. One may use {@link java.util.TreeMap} and
     * {@link java.util.TreeSet} in combination with this {@link Comparator} to implement the before-mentioned
     * operations.
     * </p>
     * @return the default comparator for {@link Datum}. The comparator orders null values first.
     * @see Datum
     * @see java.util.TreeSet
     * @see java.util.TreeMap
     */
    @NotNull
    static Comparator<Datum> comparator() {
        return comparator(true);
    }

    /**
     * Comparator for PartiQL values.
     * <p>
     * This may be used for the comparison operators, GROUP BY, ORDER BY, and DISTINCT. The conventional use
     * of {@link java.util.HashMap}, {@link java.util.HashSet}, {@link Object#hashCode()}, and
     * {@link Object#equals(Object)} will not work outright with Datum to implement the before-mentioned operations due
     * to requirements by the PartiQL and SQL Specifications. One may use {@link java.util.TreeMap} and
     * {@link java.util.TreeSet} in combination with this {@link Comparator} to implement the before-mentioned
     * operations.
     * </p>
     * @param nullsFirst if true, nulls are ordered before non-null values, otherwise after.
     * @return the default comparator for {@link Datum}.
     * @see Datum
     * @see java.util.TreeSet
     * @see java.util.TreeMap
     */
    @NotNull
    static Comparator<Datum> comparator(boolean nullsFirst) {
        if (nullsFirst) {
            return new DatumComparator.NullsFirst();
        } else {
            return new DatumComparator.NullsLast();
        }
    }
}
