package org.partiql.spi.value;

import com.amazon.ionelement.api.AnyElement;
import com.amazon.ionelement.api.ElementLoader;
import com.amazon.ionelement.api.IonElementLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.errors.PRuntimeException;
import org.partiql.spi.internal.value.ion.IonVariant;
import org.partiql.spi.types.IntervalCode;
import org.partiql.spi.types.PType;
import org.partiql.spi.types.PTypeField;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a representation of a value in PartiQL's type system. The intention of this modeling is to
 * provide a layer of indirection between PartiQL's type semantics and Java's type semantics.
 */
public interface Datum extends Iterable<Datum> {
    // TODO: Annotations?

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
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default String getString() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getString");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#BOOL}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default boolean getBoolean() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getBoolean");
    }

    /**
     * <p>
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * </p>
     * @return the underlying value applicable to the types:
     * {@link PType#BLOB},
     * {@link PType#CLOB}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     * in a fashion that can support much larger values -- this may be modified at any time.
     */
    @NotNull
    default byte[] getBytes() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getBytes");
    }

    /**
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * @return the underlying value applicable to the types:
     * {@link PType#TINYINT}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     * Please abstain from using this API until given notice otherwise. This may break between iterations without prior notice.
     */
    default byte getByte() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getByte");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#SMALLINT}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default short getShort() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getShort");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#INTEGER}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default int getInt() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getInt");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#BIGINT}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default long getLong() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getLong");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#REAL}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default float getFloat() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getFloat");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#DOUBLE}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default double getDouble() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getDouble");
    }

    /**
     * @return the underlying value applicable to the types:
     * {@link PType#DECIMAL}, {@link PType#NUMERIC}
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default BigDecimal getBigDecimal() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getBigDecimal");
    }

    /**
     * @return a {@link LocalDate} for DATE, TIMESTAMP, and TIMESTAMPZ types.
     * @throws InvalidOperationException if type not in (DATE, TIMESTAMP, TIMESTAMPZ)
     * @throws NullPointerException          if isNull() is true; callers should check to avoid NPEs.
     */
    @NotNull
    default LocalDate getLocalDate() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getLocalDate");
    }

    /**
     * @return an {@link OffsetTime} for TIME, TIMEZ, TIMESTAMP, TIMESTAMPZ types.
     * @throws InvalidOperationException if type not in (TIME, TIMEZ, TIMESTAMP, TIMESTAMPZ)
     * @throws NullPointerException          if isNull() is true; callers should check to avoid NPEs.
     */
    @NotNull
    default LocalTime getLocalTime() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getLocalTime");
    }

    /**
     * @return an {@link OffsetTime} for TIMEZ and TIMESTAMPZ types.
     * @throws InvalidOperationException if type not in (TIMEZ, TIMESTAMPZ)
     * @throws NullPointerException          if isNull() is true; callers should check to avoid NPEs.
     */
    @NotNull
    default OffsetTime getOffsetTime() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getOffsetTime");
    }

    /**
     * @return a {@link LocalDateTime} for TIMESTAMP, TIMESTAMPZ types.
     * @throws InvalidOperationException if type not in (TIMESTAMP, TIMESTAMPZ)
     * @throws NullPointerException          if isNull() is true; callers should check to avoid NPEs.
     */
    @NotNull
    default LocalDateTime getLocalDateTime() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getLocalDateTime");
    }

    /**
     * @return a {@link OffsetDateTime} for TIMESTAMPZ types.
     * @throws InvalidOperationException if type not TIMESTAMPZ
     * @throws NullPointerException          if isNull() is true; callers should check to avoid NPEs.
     */
    @NotNull
    default OffsetDateTime getOffsetDateTime() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getOffsetDateTime");
    }

    /**
     * <p>
     * Returns the normalized years field of the year-month INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_YM}. For example, an `INTERVAL '15' MONTH` will be normalized to
     * an `INTERVAL '1-3' YEAR TO MONTH` value. This function will return the value `1`.
     * </p>
     * @return the normalized years field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_YM}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getYears() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getYears");
    }

    /**
     * <p>
     * Returns the normalized months field of the year-month INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_YM}. For example, an `INTERVAL '15' MONTH` will be normalized to
     * an `INTERVAL '1-3' YEAR TO MONTH` value. This function will return the value `3`. Users can use {@link Datum#getTotalMonths()}
     * to extract the unnormalized total amount of months.
     * </p>
     * @return the normalized months field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_YM}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getMonths() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getMonths");
    }

    /**
     * <p>
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * </p>
     * <p>
     * Returns the total amount of months for this year-month INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_YM}.
     * </p>
     * @return the total number of months.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_YM}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default long getTotalMonths() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getTotalMonths");
    }

    /**
     * <p>
     * <b>! ! ! EXPERIMENTAL ! ! !</b> This is an experimental API under development by the PartiQL maintainers.
     * </p>
     * <p>
     * Returns the total amount of whole seconds for this day-time INTERVAL. This does not include the nanosecond
     * component of the day-time INTERVAL. The nanosecond component can be retrieved by calling {@link Datum#getNanos()}.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}.
     * </p>
     * @return the total number of whole seconds.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default long getTotalSeconds() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getTotalSeconds");
    }

    /**
     * <p>
     * Returns the normalized days field of the day-time INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}. For example, an `INTERVAL '30' HOURS` will be normalized to
     * an `INTERVAL '1 6:0:0' DAY TO SECOND` value. This function will return the value `1`. Users can use {@link Datum#getTotalSeconds()}
     * to extract the unnormalized total amount of days.
     * </p>
     * @return the normalized days field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getDays() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getDays");
    }

    /**
     * <p>
     * Returns the hours field of the day-time INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}. For example, an `INTERVAL '90' MINUTES` will be normalized to
     * an `INTERVAL '0 1:30:0' DAY TO SECOND` value. This function will return the value `1`. Users can use {@link Datum#getTotalSeconds()}
     * to extract the unnormalized total amount of hours.
     * </p>
     * @return the normalized hours field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getHours() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getHours");
    }

    /**
     * <p>
     * Returns the normalized minutes field of the day-time INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}. For example, an `INTERVAL '90' MINUTE` will be normalized to
     * an `INTERVAL '0 1:30:0' DAY TO SECOND` value. This function will return the value `30`. Users can use {@link Datum#getTotalSeconds()}
     * to extract the unnormalized total amount of minutes.
     * </p>
     * @return the normalized minutes field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getMinutes() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getMinutes");
    }

    /**
     * <p>
     * Returns the normalized seconds field of the day-time INTERVAL.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}. For example, an `INTERVAL '90' SECOND` will be normalized to
     * an `INTERVAL '0 0:1:30' DAY TO SECOND` value. This function will return the value `30`. Users can use {@link Datum#getTotalSeconds()}
     * to extract the unnormalized total amount of seconds.
     * </p>
     * @return the normalized seconds field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getSeconds() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getSeconds");
    }

    /**
     * <p>
     * Returns the nanos field of the interval type. Note that this must be used in combination with
     * {@link PType#getFractionalPrecision()}.
     * </p>
     * <p>
     * This only applies to {@link PType#INTERVAL_DT}.
     * </p>
     * @return the nanos field of the interval type.
     * @throws InvalidOperationException when the type is not {@link PType#INTERVAL_DT}.
     * @throws NullPointerException when {@link #isNull()} is true.
     */
    default int getNanos() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "getNanos");
    }

    /**
     * @return the elements of either bags/lists; returns the fields' values if the type is a struct.
     * @throws InvalidOperationException if this operation is invoked on a value that is not of the following
     *                                       types: {@link PType#BAG}, {@link PType#ARRAY}, and
     *                                       {@link PType#STRUCT}.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    @Override
    default Iterator<Datum> iterator() {
        throw new InvalidOperationException(getType(), "iterator");
    }

    /**
     * @return the underlying values applicable to the type {@link PType#STRUCT}.
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    @NotNull
    default Iterator<Field> getFields() {
        throw new InvalidOperationException(getType(), "getFields");
    }

    /**
     * @return the underlying value applicable to the type {@link PType#STRUCT} and requested field name. This
     * is a case-sensitive lookup.
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum get(@NotNull String name) {
        throw new InvalidOperationException(getType(), "get");
    }

    /**
     * @return the underlying value applicable to the type {@link PType#STRUCT} and requested field name. This
     * is a case-insensitive lookup.
     * @throws InvalidOperationException if the operation is not applicable to the type returned from
     *                                       {@link #getType()}; for example, if {@link #getType()} returns a {@link PType#INTEGER}, then this method
     *                                       will throw this exception upon invocation.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum getInsensitive(@NotNull String name) {
        throw new InvalidOperationException(getType(), "getInsensitive");
    }

    /**
     * Pack a VARIANT into a byte array with the given charset.
     *
     * @param charset optional charset.
     * @return the variant as an encoded byte[].
     * @throws InvalidOperationException if the datum is not a VARIANT.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default byte[] pack(@Nullable Charset charset) {
        throw new InvalidOperationException(getType(), "pack");
    }

    /**
     * Lower a VARIANT into a non-VARIANT datum.
     *
     * @return a non-VARIANT datum.
     * @throws InvalidOperationException if the datum is not a VARIANT.
     * @throws NullPointerException          if this instance also returns true on {@link #isNull()}; callers should check that
     *                                       {@link #isNull()} returns false before attempting to invoke this method.
     */
    default Datum lower() throws InvalidOperationException, NullPointerException {
        throw new InvalidOperationException(getType(), "lower");
    }

    /**
     * @return a null value with type {@link PType#UNKNOWN}.
     */
    @NotNull
    static Datum nullValue() {
        return new DatumNull();
    }

    /**
     * @return a missing value with type {@link PType#UNKNOWN}.
     */
    @NotNull
    static Datum missing() {
        return new DatumMissing();
    }

    /**
     * @param type the type of the value
     * @return a typed null value
     * @throws PRuntimeException if the type is not a runtime type.
     */
    @NotNull
    static Datum nullValue(@NotNull PType type) throws PRuntimeException {
        return new DatumNull(type);
    }

    /**
     * @param type the type of the value
     * @return a typed missing value
     * @throws PRuntimeException if the type is not a runtime type.
     */
    @NotNull
    static Datum missing(@NotNull PType type) throws PRuntimeException {
        return new DatumMissing(type);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#BOOL}
     */
    @NotNull
    static Datum bool(boolean value) {
        return new DatumBoolean(value);
    }

    // NUMERIC

    /**
     * @param value the backing value
     * @return a value of type {@link PType#TINYINT}
     */
    @NotNull
    static Datum tinyint(byte value) {
        return new DatumByte(value, PType.tinyint());
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#SMALLINT}
     */
    @NotNull
    static Datum smallint(short value) {
        return new DatumShort(value);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#INTEGER}
     */
    @NotNull
    static Datum integer(int value) {
        return new DatumInt(value);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#BIGINT}
     */
    @NotNull
    static Datum bigint(long value) {
        return new DatumLong(value);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#REAL}
     */
    @NotNull
    static Datum real(float value) {
        return new DatumFloat(value);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#DOUBLE}
     */
    @NotNull
    static Datum doublePrecision(double value) {
        return new DatumDouble(value);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#DECIMAL} with the default precision/scale
     * @throws PRuntimeException with {@link org.partiql.spi.errors.PError#NUMERIC_VALUE_OUT_OF_RANGE} if the value could not fit into the requested precision/scale
     */
    @NotNull
    static Datum decimal(@NotNull BigDecimal value) throws PRuntimeException {
        return new DatumDecimal(value, PType.decimal(38, 0));
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @param scale the scale to coerce the value to
     * @return a value of type {@link PType#DECIMAL} with the requested precision/scale
     * @throws PRuntimeException with {@link org.partiql.spi.errors.PError#NUMERIC_VALUE_OUT_OF_RANGE} if the value could not fit into the requested precision/scale
     */
    @NotNull
    static Datum decimal(@NotNull BigDecimal value, int precision, int scale) throws PRuntimeException {
        BigDecimal d = value.round(new MathContext(precision)).setScale(scale, RoundingMode.HALF_UP);
        PType type = PType.decimal(precision, scale);
        if (d.precision() > precision) {
            throw PErrors.numericValueOutOfRangeException(value.toString(), type);
        }
        return new DatumDecimal(d, type);
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#NUMERIC} with the default precision/scale
     * @throws PRuntimeException with {@link org.partiql.spi.errors.PError#NUMERIC_VALUE_OUT_OF_RANGE} if the value could not fit into the default precision/scale
     */
    @NotNull
    static Datum numeric(@NotNull BigDecimal value) throws PRuntimeException {
        return new DatumDecimal(value, PType.numeric());
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @param scale the scale to coerce the value to
     * @return a value of type {@link PType#NUMERIC} with the requested precision/scale
     * @throws PRuntimeException with {@link org.partiql.spi.errors.PError#NUMERIC_VALUE_OUT_OF_RANGE} if the value could not fit into the requested precision/scale
     */
    @NotNull
    static Datum numeric(@NotNull BigDecimal value, int precision, int scale) throws PRuntimeException {
        BigDecimal d = value.round(new MathContext(precision)).setScale(scale, RoundingMode.HALF_UP);
        PType type = PType.numeric(precision, scale);
        if (d.precision() > precision) {
            throw PErrors.numericValueOutOfRangeException(value.toString(), type);
        }
        return new DatumDecimal(d, type);
    }

    // CHARACTER STRINGS

    /**
     * @param value the backing value
     * @return a value of type {@link PType#STRING}
     */
    @NotNull
    static Datum string(@NotNull String value) {
        return new DatumString(value, PType.string());
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#VARCHAR} with the default length
     * @throws PRuntimeException if the value could not fit into the default length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum varchar(@NotNull String value) throws PRuntimeException {
        // TODO: Add an ErrorCode for when we can't fit the value into the requested length
        return varchar(value, 255);
    }

    /**
     * @param value the backing value
     * @param length the length of the varchar to coerce the value to
     * @return a value of type {@link PType#VARCHAR} with the requested length
     * @throws PRuntimeException if the value could not fit into the requested length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum varchar(@NotNull String value, int length) throws PRuntimeException {
        // TODO: Error or coerce here? Right now coerce, though I think this should likely error.
        String newValue;
        if (length <= 0) {
            throw PErrors.wrappedException(new IllegalArgumentException("VARCHAR of length " + length + " not allowed."));
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
     * @param value the backing value
     * @return a value of type {@link PType#CHAR} with the default length
     * @throws PRuntimeException if the value could not fit into the default length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum character(@NotNull String value) throws PRuntimeException {
        return character(value, 255);
    }

    /**
     * @param value the backing value
     * @param length the length of the char to coerce the value to
     * @return a value of type {@link PType#CHAR} with the default length
     * @throws PRuntimeException if the value could not fit into the requested length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum character(@NotNull String value, int length) throws PRuntimeException {
        // TODO: Error or coerce here? Right now coerce, though I think this should likely error.
        String newValue;
        if (length <= 0) {
            throw PErrors.wrappedException(new IllegalArgumentException("CHAR of length " + length + " not allowed."));
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

    /**
     * @param value the backing value
     * @return a value of type {@link PType#CLOB} with the default length
     * @throws PRuntimeException if the value could not fit into the default length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum clob(@NotNull byte[] value) throws PRuntimeException {
        // TODO: Check size of value
        return clob(value, Long.MAX_VALUE);
    }

    /**
     * @param value the backing value
     * @param length the length of the clob to coerce the value to
     * @return a value of type {@link PType#CLOB} with the default length
     * @throws PRuntimeException if the value could not fit into the requested length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum clob(@NotNull byte[] value, long length) throws PRuntimeException {
        // TODO: Check size of value
        return new DatumBytes(value, PType.clob(length));
    }

    // BYTE STRINGS

    /**
     * @param value the backing value
     * @return a value of type {@link PType#BLOB} with the default length
     * @throws PRuntimeException if the value could not fit into the default length, or if the requested length is not allowed ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum blob(@NotNull byte[] value) throws PRuntimeException {
        // TODO: Check size
        return new DatumBytes(value, PType.blob(Integer.MAX_VALUE));
    }

    /**
     * @param value the backing value
     * @param length the length of the clob to coerce the value to
     * @return a value of type {@link PType#BLOB} with the default length
     * @throws PRuntimeException if the value could not fit into the requested length, or if the length is not valid ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum blob(@NotNull byte[] value, int length) throws PRuntimeException {
        // TODO: Check size
        return new DatumBytes(value, PType.blob(length));
    }

    // DATE/TIME

    /**
     * @param value the backing value
     * @return a value of type {@link PType#DATE}
     * @throws PRuntimeException if the value could not be converted to a date.
     */
    @NotNull
    static Datum date(@NotNull LocalDate value) throws PRuntimeException {
        return new DatumDate(value);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIME}
     * @throws PRuntimeException if the value could not fit into the requested precision, or if the precision is not valid ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum time(@NotNull LocalTime value, int precision) throws PRuntimeException {
        // TODO: Check precision
        return new DatumTime(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMEZ}
     * @throws PRuntimeException if the value could not fit into the requested precision, or if the precision is not valid ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum timez(@NotNull OffsetTime value, int precision) throws PRuntimeException {
        // TODO: Check precision
        return new DatumTimez(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMESTAMP}
     * @throws PRuntimeException if the value could not fit into the requested precision, or if the precision is not valid ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum timestamp(@NotNull LocalDateTime value, int precision) throws PRuntimeException {
        // TODO: Check precision
        return new DatumTimestamp(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMESTAMPZ}
     * @throws PRuntimeException if the value could not fit into the requested precision, or if the precision is not valid ({@link org.partiql.spi.errors.PError#INTERNAL_ERROR})
     */
    @NotNull
    static Datum timestampz(@NotNull OffsetDateTime value, int precision) throws PRuntimeException {
        // TODO: Check precision
        return new DatumTimestampz(value, precision);
    }

    // COLLECTIONS

    /**
     * @param values the backing values
     * @return a value of type {@link PType#BAG}
     */
    @NotNull
    static Datum bag(@NotNull Iterable<Datum> values) {
        return new DatumCollection(values, PType.bag());
    }

    /**
     * @param values the backing values
     * @return a value of type {@link PType#BAG}
     */
    @NotNull
    static Datum bagVararg(@NotNull Datum... values) {
        // Developer note: This needs to be named this way to disambiguate from the above method. Datum is an Iterable<Datum>
        List<Datum> elements = Arrays.stream(values).collect(Collectors.toList());
        return new DatumCollection(elements, PType.bag());
    }

    /**
     * @param values the backing values
     * @return a value of type {@link PType#ARRAY}
     */
    @NotNull
    static Datum array(@NotNull Iterable<Datum> values) {
        return new DatumCollection(values, PType.array());
    }

    // STRUCTURAL

    /**
     * @return a value of type {@link PType#STRUCT}
     */
    @NotNull
    static Datum struct() {
        return struct(Collections.emptyList());
    }

    /**
     * @param values the backing values
     * @return a value of type {@link PType#STRUCT}
     */
    @NotNull
    static Datum struct(@NotNull Field... values) {
        return new DatumStruct(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * @param values the backing values
     * @return a value of type {@link PType#STRUCT}
     */
    @NotNull
    static Datum struct(@NotNull Iterable<Field> values) {
        return new DatumStruct(values);
    }

    /**
     * Returns an empty {@link PType#ROW}
     * @return a value of type {@link PType#ROW}
     */
    @NotNull
    static Datum row() {
        return new DatumRow(new ArrayList<>(), PType.row());
    }

    /**
     * This creates a row.
     * @param values the backing values
     * @return a value of type {@link PType#ROW}
     */
    @NotNull
    static Datum row(@NotNull Field... values) {
        return new DatumRow(Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * This creates a row. Use this if you'd like to save on the computational cost of computing the final type.
     * @param typeFields the backing type fields
     * @param values the backing values
     * @return a value of type {@link PType#ROW}
     */
    @NotNull
    static Datum row(List<PTypeField> typeFields, @NotNull Field... values) {
        return row(typeFields, Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * Creates a row with the given values.
     * @param values the backing values
     * @return a value of type {@link PType#ROW}
     */
    @NotNull
    static Datum row(@NotNull List<Field> values) {
        return new DatumRow(values);
    }

    /**
     * Creates a row with the given values. Use this if you'd like to save on the computational cost of computing the final type.
     * @param typeFields the backing type fields
     * @param values the backing values
     * @return a value of type {@link PType#ROW}
     */
    @NotNull
    static Datum row(@NotNull List<PTypeField> typeFields, @NotNull List<Field> values) {
        PType type = PType.row(typeFields);
        return new DatumRow(values, type);
    }

    /**
     * @param value the backing Ion
     * @return a value of type {@link PType#VARIANT}
     * @throws PRuntimeException if the value could not be converted to a variant. Possible codes: {@link org.partiql.spi.errors.PError#INTERNAL_ERROR}
     */
    @NotNull
    static Datum ion(@NotNull String value) throws PRuntimeException {
        try {
            IonElementLoader loader = ElementLoader.createIonElementLoader();
            AnyElement element = loader.loadSingleElement(value);
            return new IonVariant(element);
        } catch (Throwable t) {
            throw PErrors.wrappedException(t);
        }
    }

    /**
     * Creates a value of type INTERVAL YEAR (precision)
     * @param years the number of years in this interval
     * @param precision the number of decimal digits allowed to represent YEAR
     * @return a value of type INTERVAL YEAR (precision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalYear(int years, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(years, precision);
        return new DatumIntervalYearMonth(years, 0, precision, IntervalCode.YEAR);
    }

    /**
     * Creates a value of type INTERVAL MONTH (precision)
     * @param months the number of months in this interval
     * @param precision the number of decimal digits allowed to represent MONTH
     * @return a value of type INTERVAL MONTH (precision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalMonth(int months, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(months, precision);
        return new DatumIntervalYearMonth(0, months, precision, IntervalCode.MONTH);
    }

    /**
     * Creates a value of type INTERVAL DAY (precision)
     * @param days the number of days in this interval
     * @param precision the number of decimal digits allowed to represent DAY
     * @return a value of type INTERVAL DAY (precision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalDay(int days, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(days, precision);
        return new DatumIntervalDayTime(days, 0, 0, 0, 0, precision, 0, IntervalCode.DAY);
    }

    /**
     * Creates a value of type INTERVAL HOUR (precision)
     * @param hours the number of hours in this interval
     * @param precision the number of decimal digits allowed to represent HOUR
     * @return a value of type INTERVAL HOUR (precision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalHour(int hours, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(hours, precision);
        return new DatumIntervalDayTime(0, hours, 0, 0, 0, precision, 0, IntervalCode.HOUR);
    }

    /**
     * Creates a value of type INTERVAL MINUTE (precision)
     * @param minutes the number of minutes in this interval
     * @param precision the number of decimal digits allowed to represent MINUTE
     * @return a value of type INTERVAL MINUTE (precision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalMinute(int minutes, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(minutes, precision);
        return new DatumIntervalDayTime(0, 0, minutes, 0, 0, precision, 0, IntervalCode.MINUTE);
    }

    /**
     * Creates a value of type INTERVAL SECOND (precision, fractionalPrecision)
     * @param seconds the number of seconds in this interval
     * @param nanos the number of nanoseconds in this interval
     * @param precision the number of decimal digits allowed to represent SECOND
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for nanoseconds
     * @return a value of type INTERVAL SECOND (precision, fractionalPrecision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalSecond(int seconds, int nanos, int precision, int fractionalPrecision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkScale(fractionalPrecision);
        DatumIntervalHelpers.checkUsingPrecision(seconds, precision);
        int newNanos = DatumIntervalHelpers.coerceNanos(nanos, fractionalPrecision);
        return new DatumIntervalDayTime(0, 0, 0, seconds, newNanos, precision, fractionalPrecision, IntervalCode.SECOND);
    }

    /**
     * Creates a value of type INTERVAL YEAR (precision) TO MONTH
     * @param years the number of years in this interval
     * @param months the number of months in this interval
     * @param precision the number of decimal digits allowed to represent YEAR
     * @return a value of type INTERVAL YEAR (precision) TO MONTH
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalYearMonth(int years, int months, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(years, precision);
        DatumIntervalHelpers.checkMonths(months);
        return new DatumIntervalYearMonth(years, months, precision, IntervalCode.YEAR_MONTH);
    }

    /**
     * Creates a value of type INTERVAL DAY (precision) TO HOUR
     * @param days the number of days in this interval
     * @param hours the number of hours in this interval
     * @param precision the number of decimal digits allowed to represent DAY
     * @return a value of type INTERVAL DAY (precision) TO HOUR
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalDayHour(int days, int hours, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(days, precision);
        DatumIntervalHelpers.checkHours(hours);
        return new DatumIntervalDayTime(days, hours, 0, 0, 0, precision, 0, IntervalCode.DAY_HOUR);
    }

    /**
     * Creates a value of type INTERVAL DAY (precision) TO MINUTE
     * @param days the number of days in this interval
     * @param hours the number of hours in this interval
     * @param minutes the number of minutes in this interval
     * @param precision the number of decimal digits allowed to represent DAY
     * @return a value of type INTERVAL DAY (precision) TO MINUTE
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalDayMinute(int days, int hours, int minutes, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(days, precision);
        DatumIntervalHelpers.checkHours(hours);
        DatumIntervalHelpers.checkMinutes(minutes);
        return new DatumIntervalDayTime(days, hours, minutes, 0, 0, precision, 0, IntervalCode.DAY_MINUTE);
    }

    /**
     * Creates a value of type INTERVAL DAY (precision) TO SECOND (fractionalPrecision)
     * @param days the number of days in this interval
     * @param hours the number of hours in this interval
     * @param minutes the number of minutes in this interval
     * @param seconds the number of seconds in this interval
     * @param nanos the number of nanoseconds in this interval
     * @param precision the number of decimal digits allowed to represent DAY
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS
     * @return a value of type INTERVAL DAY (precision) TO SECOND (fractionalPrecision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalDaySecond(int days, int hours, int minutes, int seconds, int nanos, int precision, int fractionalPrecision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkScale(fractionalPrecision);
        DatumIntervalHelpers.checkUsingPrecision(days, precision);
        DatumIntervalHelpers.checkHours(hours);
        DatumIntervalHelpers.checkMinutes(minutes);
        DatumIntervalHelpers.checkSeconds(seconds);
        int newNanos = DatumIntervalHelpers.coerceNanos(nanos, fractionalPrecision);
        return new DatumIntervalDayTime(days, hours, minutes, seconds, newNanos, precision, fractionalPrecision, IntervalCode.DAY_SECOND);
    }

    /**
     * Creates a value of type INTERVAL HOUR (precision) TO MINUTE
     * @param hours the number of hours in this interval
     * @param minutes the number of minutes in this interval
     * @param precision the number of decimal digits allowed to represent HOUR
     * @return a value of type INTERVAL HOUR (precision) TO MINUTE
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalHourMinute(int hours, int minutes, int precision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkUsingPrecision(hours, precision);
        DatumIntervalHelpers.checkMinutes(minutes);
        return new DatumIntervalDayTime(0, hours, minutes, 0, 0, precision, 0, IntervalCode.HOUR_MINUTE);
    }

    /**
     * Creates a value of type INTERVAL HOUR (precision) TO SECOND (fractionalPrecision)
     * @param hours the number of hours in this interval
     * @param minutes the number of minutes in this interval
     * @param seconds the number of seconds in this interval
     * @param nanos the number of nanoseconds in this interval
     * @param precision the number of decimal digits allowed to represent HOUR
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS
     * @return a value of type INTERVAL HOUR (precision) TO SECOND (fractionalPrecision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalHourSecond(int hours, int minutes, int seconds, int nanos, int precision, int fractionalPrecision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkScale(fractionalPrecision);
        DatumIntervalHelpers.checkUsingPrecision(hours, precision);
        DatumIntervalHelpers.checkMinutes(minutes);
        DatumIntervalHelpers.checkSeconds(seconds);
        int newNanos = DatumIntervalHelpers.coerceNanos(nanos, fractionalPrecision);
        return new DatumIntervalDayTime(0, hours, minutes, seconds, newNanos, precision, fractionalPrecision, IntervalCode.HOUR_SECOND);
    }

    /**
     * Creates a value of type INTERVAL MINUTE (precision) TO SECOND (fractionalPrecision)
     * @param minutes the number of minutes in this interval
     * @param seconds the number of seconds in this interval
     * @param nanos the number of nanoseconds in this interval
     * @param precision the number of decimal digits allowed to represent MINUTE
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS
     * @return a value of type INTERVAL MINUTE (precision) TO SECOND (fractionalPrecision)
     * @throws RuntimeException if an error occurs.
     */
    @NotNull
    static Datum intervalMinuteSecond(int minutes, int seconds, int nanos, int precision, int fractionalPrecision) throws RuntimeException {
        DatumIntervalHelpers.checkPrecision(precision);
        DatumIntervalHelpers.checkScale(fractionalPrecision);
        DatumIntervalHelpers.checkUsingPrecision(minutes, precision);
        DatumIntervalHelpers.checkSeconds(seconds);
        int newNanos = DatumIntervalHelpers.coerceNanos(nanos, fractionalPrecision);
        return new DatumIntervalDayTime(0, 0, minutes, seconds, newNanos, precision, fractionalPrecision, IntervalCode.MINUTE_SECOND);
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
        return comparator(true, false);
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
     * @return the default comparator for {@link Datum}. By default, null and missing are treated as equivalent.
     * @see Datum
     * @see java.util.TreeSet
     * @see java.util.TreeMap
     */
    @NotNull
    static Comparator<Datum> comparator(boolean nullsFirst) {
        return comparator(nullsFirst, false);
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
     * @param distinguishNullMissing if true, null and missing values are distinguished (null < missing), otherwise they are treated as equivalent. By default, this is set to false and only set to true when testing for now.
     * @return the comparator for {@link Datum}.
     * @see Datum
     * @see java.util.TreeSet
     * @see java.util.TreeMap
     */
    @NotNull
    static Comparator<Datum> comparator(boolean nullsFirst, boolean distinguishNullMissing) {
        if (nullsFirst) {
            return new DatumComparator.NullsFirst(distinguishNullMissing);
        } else {
            return new DatumComparator.NullsLast(distinguishNullMissing);
        }
    }
}
