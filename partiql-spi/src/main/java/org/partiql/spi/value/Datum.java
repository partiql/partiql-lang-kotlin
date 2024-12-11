package org.partiql.spi.value;

import com.amazon.ionelement.api.AnyElement;
import com.amazon.ionelement.api.ElementLoader;
import com.amazon.ionelement.api.IonElementLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.errors.DataException;
import org.partiql.spi.value.ion.IonVariant;
import org.partiql.types.PType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
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
        throw new UnsupportedOperationException("Has type: " + getType() + " and class " + this.getClass().getName());
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
     * @param type the type of the value
     * @return a typed missing value
     */
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
    static Datum time(@NotNull Time value, int precision) {
        return new DatumTime(value, precision);
    }

    @NotNull
    static Datum timestamp(@NotNull Timestamp value) {
        return new DatumTimestamp(value);
    }

    @NotNull
    static Datum timestamp(@NotNull Timestamp value, int precision) {
        return new DatumTimestamp(value, precision);
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
