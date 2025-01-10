package org.partiql.spi.value;

import com.amazon.ionelement.api.AnyElement;
import com.amazon.ionelement.api.ElementLoader;
import com.amazon.ionelement.api.IonElementLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.errors.DataException;
import org.partiql.spi.internal.value.ion.IonVariant;
import org.partiql.spi.types.PType;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

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
     */
    @NotNull
    static Datum nullValue(@NotNull PType type) {
        return new DatumNull(type);
    }

    /**
     * @param type the type of the value
     * @return a typed missing value
     */
    @NotNull
    static Datum missing(@NotNull PType type) {
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
     */
    @NotNull
    static Datum decimal(@NotNull BigDecimal value) {
        return new DatumDecimal(value, PType.decimal(38, 0));
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @param scale the scale to coerce the value to
     * @return a value of type {@link PType#DECIMAL} with the requested precision/scale
     * @throws DataException if the value could not fit into the requested precision/scale
     */
    @NotNull
    static Datum decimal(@NotNull BigDecimal value, int precision, int scale) throws DataException {
        BigDecimal d = value.round(new MathContext(precision)).setScale(scale, RoundingMode.HALF_UP);
        if (d.precision() > precision) {
            throw new DataException("Value " + d + " could not fit into decimal with precision " + precision + " and scale " + scale + ".");
        }
        return new DatumDecimal(d, PType.decimal(precision, scale));
    }

    /**
     * @param value the backing value
     * @return a value of type {@link PType#NUMERIC} with the default precision/scale
     */
    @NotNull
    static Datum numeric(@NotNull BigDecimal value) {
        return new DatumDecimal(value, PType.numeric());
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @param scale the scale to coerce the value to
     * @return a value of type {@link PType#NUMERIC} with the requested precision/scale
     * @throws DataException if the value could not fit into the requested precision/scale
     */
    @NotNull
    static Datum numeric(@NotNull BigDecimal value, int precision, int scale) throws DataException {
        BigDecimal d = value.round(new MathContext(precision)).setScale(scale, RoundingMode.HALF_UP);
        if (d.precision() > precision) {
            throw new DataException("Value " + d + " could not fit into numeric with precision " + precision + " and scale " + scale + ".");
        }
        return new DatumDecimal(d, PType.numeric(precision, scale));
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
     * @throws DataException if the value could not fit into the default length
     */
    @NotNull
    static Datum varchar(@NotNull String value) throws DataException {
        return varchar(value, 255);
    }

    /**
     * @param value the backing value
     * @param length the length of the varchar to coerce the value to
     * @return a value of type {@link PType#VARCHAR} with the requested length
     * @throws DataException if the value could not fit into the requested length
     */
    @NotNull
    static Datum varchar(@NotNull String value, int length) throws DataException {
        // TODO: Error or coerce here? Right now coerce, though I think this should likely error.
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
     * @param value the backing value
     * @return a value of type {@link PType#CHAR} with the default length
     * @throws DataException if the value could not fit into the default length
     */
    @NotNull
    static Datum character(@NotNull String value) throws DataException {
        return character(value, 255);
    }

    /**
     * @param value the backing value
     * @param length the length of the char to coerce the value to
     * @return a value of type {@link PType#CHAR} with the default length
     * @throws DataException if the value could not fit into the requested length
     */
    @NotNull
    static Datum character(@NotNull String value, int length) throws DataException {
        // TODO: Error or coerce here? Right now coerce, though I think this should likely error.
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

    /**
     * @param value the backing value
     * @return a value of type {@link PType#CLOB} with the default length
     * @throws DataException if the value could not fit into the default length
     */
    @NotNull
    static Datum clob(@NotNull byte[] value) throws DataException {
        // TODO: Check size of value
        return clob(value, Integer.MAX_VALUE);
    }

    /**
     * @param value the backing value
     * @param length the length of the clob to coerce the value to
     * @return a value of type {@link PType#CLOB} with the default length
     * @throws DataException if the value could not fit into the requested length
     */
    @NotNull
    static Datum clob(@NotNull byte[] value, int length) throws DataException {
        // TODO: Check size of value
        return new DatumBytes(value, PType.clob(length));
    }

    // BYTE STRINGS

    /**
     * @param value the backing value
     * @return a value of type {@link PType#BLOB} with the default length
     * @throws DataException if the value could not fit into the default length
     */
    @NotNull
    static Datum blob(@NotNull byte[] value) {
        // TODO: Check size
        return new DatumBytes(value, PType.blob(Integer.MAX_VALUE));
    }

    /**
     * @param value the backing value
     * @param length the length of the clob to coerce the value to
     * @return a value of type {@link PType#BLOB} with the default length
     * @throws DataException if the value could not fit into the requested length
     */
    @NotNull
    static Datum blob(@NotNull byte[] value, int length) throws DataException {
        // TODO: Check size
        return new DatumBytes(value, PType.blob(length));
    }

    // DATE/TIME

    /**
     * @param value the backing value
     * @return a value of type {@link PType#DATE}
     */
    @NotNull
    static Datum date(@NotNull LocalDate value) {
        return new DatumDate(value);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIME}
     * @throws DataException if the value could not fit into the requested precision
     */
    @NotNull
    static Datum time(@NotNull LocalTime value, int precision) throws DataException {
        // TODO: Check precision
        return new DatumTime(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMEZ}
     * @throws DataException if the value could not fit into the requested precision
     */
    @NotNull
    static Datum timez(@NotNull OffsetTime value, int precision) throws DataException {
        // TODO: Check precision
        return new DatumTimez(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMESTAMP}
     * @throws DataException if the value could not fit into the requested precision
     */
    @NotNull
    static Datum timestamp(@NotNull LocalDateTime value, int precision) throws DataException {
        // TODO: Check precision
        return new DatumTimestamp(value, precision);
    }

    /**
     * @param value the backing value
     * @param precision the precision to coerce the value to
     * @return a value of type {@link PType#TIMESTAMPZ}
     * @throws DataException if the value could not fit into the requested precision
     */
    @NotNull
    static Datum timestampz(@NotNull OffsetDateTime value, int precision) throws DataException {
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
    static Datum struct(@NotNull Iterable<Field> values) {
        return new DatumStruct(values);
    }

    /**
     * @param value the backing Ion
     * @return a value of type {@link PType#VARIANT}
     */
    @NotNull
    static Datum ion(@NotNull String value) {
        IonElementLoader loader = ElementLoader.createIonElementLoader();
        AnyElement element = loader.loadSingleElement(value);
        return new IonVariant(element);
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
