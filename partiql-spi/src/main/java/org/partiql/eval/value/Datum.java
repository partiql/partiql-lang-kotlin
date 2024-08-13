package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValue;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * This is an EXPERIMENTAL representation of a value in PartiQL's type system. The intention of this modeling is to
 * provide a layer of indirection between PartiQL's type semantics and Java's type semantics.
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
 *
 * @apiNote ! EXPERIMENTAL ! This API is experimental and may be removed/modified without prior notice.
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
     * {@link PType.Kind#STRING},
     * {@link PType.Kind#CHAR}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
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
     * {@link PType.Kind#BOOL}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#BLOB},
     * {@link PType.Kind#CLOB}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * @apiNote <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated BINARY doesn't exist in SQL or Ion. This is subject to deletion. BLOB and CLOB are typically represented
     * in a fashion that can support much larger values -- this may be modified at any time.
     */
    @NotNull
    default byte[] getBytes() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#TINYINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * @apiNote <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated BYTE is not present in SQL or Ion. This is subject to deletion.
     */
    @Deprecated
    default byte getByte() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#DATE}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
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
     * {@link PType.Kind#TIME}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
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
     * {@link PType.Kind#TIMESTAMP}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Timestamp getTimestamp() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * @apiNote <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * @deprecated This implementation is likely wrong and is not recommended for use.
     */
    @Deprecated
    default long getInterval() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#SMALLINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default short getShort() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#INTEGER}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default int getInt() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#BIGINT}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default long getLong() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#INTEGER}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
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
     * {@link PType.Kind#REAL}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default float getFloat() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#DOUBLE}
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default double getDouble() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType.Kind#DECIMAL},
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigDecimal getBigDecimal() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the elements of either bags, lists, or sexps; returns the fields' values if the type is a struct.
     * @throws UnsupportedOperationException if this operation is invoked on a value that is not of the following
     *                                       types: {@link PType.Kind#BAG}, {@link PType.Kind#ARRAY}, and
     *                                       {@link PType.Kind#STRUCT}.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @Override
    default Iterator<Datum> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying values applicable to the type {@link PType.Kind#STRUCT}.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<Field> getFields() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PType.Kind#STRUCT} and requested field name. This
     * is a case-sensitive lookup.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum get(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the underlying value applicable to the type {@link PType.Kind#STRUCT} and requested field name. This
     * is a case-insensitive lookup.
     * @throws UnsupportedOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType.Kind#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum getInsensitive(@NotNull String name) {
        throw new UnsupportedOperationException();
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
     *
     * @param type the type of the value
     * @return a typed missing value
     * @apiNote ! EXPERIMENTAL ! This is subject to breaking changes and/or removal without prior notice.
     * @deprecated this may not be required. This is subject to removal.
     */
    @Deprecated
    @NotNull
    static Datum missing(@NotNull PType type) {
        return new DatumMissing(type);
    }

    // BOOLEAN

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
    static Datum smallInt(short value) {
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

    @Deprecated
    @NotNull
    static Datum decimal(@NotNull BigDecimal value) {
        return new DatumDecimal(value, PType.decimal());
    }

    @NotNull
    static Datum decimal(@NotNull BigDecimal value, int precision, int scale) {
        return new DatumDecimal(value, PType.decimal(precision, scale));
    }

    @NotNull
    static Datum real(float value) {
        return new DatumFloat(value);
    }

    @NotNull
    static Datum doublePrecision(double value) {
        return new DatumDouble(value);
    }

    // CHARACTER STRINGS

    @NotNull
    static Datum string(@NotNull String value) {
        return new DatumString(value, PType.string());
    }

    @NotNull
    static Datum clob(@NotNull byte[] value) {
        return new DatumBytes(value, PType.clob(Integer.MAX_VALUE));
    }

    // BIT STRINGS

    @NotNull
    static Datum blob(@NotNull byte[] value) {
        return new DatumBytes(value, PType.blob(Integer.MAX_VALUE));
    }

    // DATETIME

    @NotNull
    static Datum time(@NotNull Time value) {
        return new DatumTime(value);
    }

    @NotNull
    static Datum timestamp(@NotNull Timestamp value) {
        return new DatumTimestamp(value);
    }

    @NotNull
    static Datum date(@NotNull Date value) {
        return new DatumDate(value);
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
    static Datum struct(@NotNull Iterable<Field> values) {
        return new DatumStruct(values);
    }

}
