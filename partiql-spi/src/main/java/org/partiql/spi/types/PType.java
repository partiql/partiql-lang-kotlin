package org.partiql.spi.types;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.UnsupportedCodeException;

import java.util.Arrays;
import java.util.Collection;

/**
 * This represents a PartiQL type, whether it be a PartiQL primitive or user-defined.
 * <p></p>
 * This implementation allows for parameterization of the core type ({@link PType#code()}) while allowing for APIs
 * to access their parameters ({@link PType#getPrecision()}, {@link PType#getTypeParameter()}, etc.)
 * <p></p>
 * Before using these methods, please be careful to read each method's documentation to ensure that it applies to the current
 * {@link PType#code()}. If one carelessly invokes the wrong method, an {@link UnsupportedOperationException} will be
 * thrown.
 * <p></p>
 * This representation of a PartiQL type is intentionally modeled as a "fat" interface -- holding all methods relevant
 * to any of the types. The maintainers of PartiQL have seen an unintentional reliance on Java's type semantics that
 * make it cumbersome (with explicit Java casts) to gain access to methods. This modeling makes it simpler for the
 * PartiQL planner to have immediate access to the available type's parameters.
 * <p></p>
 * Users should NOT author their own implementation. The current recommendation is to use the static methods
 * (exposed by this interface) to instantiate a type.
 */
public abstract class PType extends org.partiql.spi.Enum {

    /**
     * Creates an {@link java.lang.Enum} with the specified {@code code}.
     *
     * @param code the unique code of this enum.
     */
    protected PType(int code) {
        super(code);
    }

    /**
     * The fields of the type
     *
     * @throws UnsupportedOperationException if this is called on a type whose {@link PType#code()} is not:
     *                                       {@link PType#ROW}
     */
    @NotNull
    public Collection<PTypeField> getFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Gets the number of decimal digits allowed by the type. Depending on the type, this may refer to <i>all</i>
     * digits, or just the digits to the left of the decimal point.
     * </p>
     * <p>
     * Allowable types:
     * <ul>
     * <li>{@link PType#DECIMAL}: the total number of decimal digits allowed (on both sides of the decimal point).</li>
     * <li>{@link PType#INTERVAL_YM}: the total number of decimal digits allowed. There is no decimal point in this case.</li>
     * <li>{@link PType#INTERVAL_DT}: the total number of decimal digits allowed on the left of the decimal point.</li>
     * </ul>
     * </p>
     * @see #getFractionalPrecision()
     * @return decimal precision
     * @throws UnsupportedOperationException if this is called on a type whose {@link PType#code()} is not:
     *                                       {@link PType#DECIMAL}, {@link PType#TIMESTAMPZ}, {@link PType#TIMESTAMP}, {@link PType#TIMEZ},
     *                                       {@link PType#TIME}, {@link PType#REAL}, {@link PType#DOUBLE}
     */
    public int getPrecision() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The max length of the type
     *
     * @return max length of a type
     * @throws UnsupportedOperationException if this is called on a type whose {@link PType#code()} is not:
     *                                       {@link PType#CHAR}, {@link PType#CLOB}, {@link PType#BLOB}
     */
    public int getLength() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The scale of the type. Example: <code>DECIMAL(&lt;param&gt;, &lt;scale&gt;)</code>
     *
     * @return the scale of the type
     * @throws UnsupportedOperationException if this is called on a type whose {@link PType#code()} is not:
     *                                       {@link PType#DECIMAL}
     */
    public int getScale() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The type parameter of the type. Example: <code>BAG(&lt;param&gt;)</code>
     *
     * @return type parameter of the type
     * @throws UnsupportedOperationException if this is called on a type whose {@link PType#code()} is not:
     * {@link PType#ARRAY}, {@link PType#BAG}
     */
    @NotNull
    public PType getTypeParameter() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Gets the interval's code. Each code corresponds with the public static final integers within
     * {@link IntervalCode} (e.g. {@link IntervalCode#YEAR}).
     * </p>
     * <p>
     * This method is only applicable to {@link PType#INTERVAL_YM} and {@link PType#INTERVAL_DT}. See their documentation
     * for details about which codes are applicable to each.
     * </p>
     * @return the interval's code. This corresponds with the public static final integers within {@link IntervalCode}.
     * @throws UnsupportedOperationException when this is called on a type that does not apply.
     * @see PType#INTERVAL_YM
     * @see PType#INTERVAL_DT
     * @see IntervalCode
     */
    public int getIntervalCode() throws UnsupportedOperationException {
        String name;
        try {
            name = name();
        } catch (UnsupportedCodeException e) {
            name = Integer.toString(code());
        }
        throw new UnsupportedOperationException("Code " + this.getClass().getName() + "." + name + " does not support getIntervalCode().");
    }

    /**
     * <p>
     * Gets the interval's fractional field precision. This indicates the number of decimal digits maintained following
     * the decimal point in the seconds value of the interval.
     * </p>
     * <p>
     * This method is only applicable to {@link PType#INTERVAL_DT}. If {@link PType#getIntervalCode()} returns a code
     * signifying the existence of the SECONDS field (i.e. {@link IntervalCode#SECOND},
     * {@link IntervalCode#DAY_SECOND}, etc.), this may return a non-negative integer value. In all other
     * cases, this shall return 0.
     * </p>
     * @return the interval's fractional field precision.
     * @throws UnsupportedOperationException when this is called on a type that does not apply.
     * @see PType#INTERVAL_YM
     * @see PType#INTERVAL_DT
     */
    public int getFractionalPrecision() throws UnsupportedOperationException {
        String name;
        try {
            name = name();
        } catch (UnsupportedCodeException e) {
            name = Integer.toString(code());
        }
        throw new UnsupportedOperationException("Code " + this.getClass().getName() + "." + name + " does not support getLeadingPrecision().");
    }

    public static int[] codes() {
        return new int[] {
                PType.DYNAMIC,
                PType.BOOL,
                PType.TINYINT,
                PType.SMALLINT,
                PType.INTEGER,
                PType.BIGINT,
                PType.NUMERIC,
                PType.DECIMAL,
                PType.REAL,
                PType.DOUBLE,
                PType.CHAR,
                PType.VARCHAR,
                PType.STRING,
                PType.BLOB,
                PType.CLOB,
                PType.DATE,
                PType.TIME,
                PType.TIMEZ,
                PType.TIMESTAMP,
                PType.TIMESTAMPZ,
                PType.ARRAY,
                PType.BAG,
                PType.ROW,
                PType.STRUCT,
                PType.UNKNOWN,
                PType.VARIANT,
                PType.INTERVAL_YM,
                PType.INTERVAL_DT
        };
    }

    @Override
    public @NotNull String name() throws UnsupportedCodeException {
        int code = code();
        switch (code) {
            case PType.DYNAMIC:
                return "DYNAMIC";
            case PType.BOOL:
                return "BOOL";
            case PType.TINYINT:
                return "TINYINT";
            case PType.SMALLINT:
                return "SMALLINT";
            case PType.INTEGER:
                return "INTEGER";
            case PType.BIGINT:
                return "BIGINT";
            case PType.NUMERIC:
                return "NUMERIC";
            case PType.DECIMAL:
                return "DECIMAL";
            case PType.REAL:
                return "REAL";
            case PType.DOUBLE:
                return "DOUBLE";
            case PType.CHAR:
                return "CHAR";
            case PType.VARCHAR:
                return "VARCHAR";
            case PType.STRING:
                return "STRING";
            case PType.BLOB:
                return "BLOB";
            case PType.CLOB:
                return "CLOB";
            case PType.DATE:
                return "DATE";
            case PType.TIME:
                return "TIME";
            case PType.TIMEZ:
                return "TIMEZ";
            case PType.TIMESTAMP:
                return "TIMESTAMP";
            case PType.TIMESTAMPZ:
                return "TIMESTAMPZ";
            case PType.ARRAY:
                return "ARRAY";
            case PType.BAG:
                return "BAG";
            case PType.ROW:
                return "ROW";
            case PType.STRUCT:
                return "STRUCT";
            case PType.UNKNOWN:
                return "UNKNOWN";
            case PType.VARIANT:
                return "VARIANT";
            case PType.INTERVAL_YM:
                return "INTERVAL_YM";
            case PType.INTERVAL_DT:
                return "INTERVAL_DT";
            default:
                throw new UnsupportedCodeException(code);
        }
    }

    /**
     * PartiQL's dynamic type. This is solely used during compilation -- it is not a possible runtime type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>DYNAMIC</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int DYNAMIC = 0;

    /**
     * SQL's boolean type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>BOOL</code>, <code>BOOLEAN</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int BOOL = 1;

    /**
     * PartiQL's tiny integer type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TINYINT</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int TINYINT = 2;

    /**
     * SQL's small integer type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>SMALLINT</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int SMALLINT = 3;

    /**
     * SQL's integer type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>INT</code>, <code>INTEGER</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int INTEGER = 4;

    /**
     * PartiQL's big integer type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>BIGINT</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int BIGINT = 5;

    /**
     * PartiQL's big integer type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>NUMERIC</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
     */
    public static final int NUMERIC = 6;

    /**
     * SQL's decimal type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>DECIMAL(&lt;precision&gt;, &lt;scale&gt;)</code>, <code>DECIMAL(&lt;precision&gt;)</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
     */
    public static final int DECIMAL = 7;

    /**
     * SQL's real type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>REAL</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int REAL = 8;

    /**
     * SQL's double precision type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>DOUBLE PRECISION</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int DOUBLE = 9;

    /**
     * SQL's character type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>CHAR(&lt;length&gt;)</code>, <code>CHARACTER(&lt;length&gt;)</code>, <code>CHAR</code>, <code>CHARACTER</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getLength()}
     */
    public static final int CHAR = 10;

    /**
     * SQL's character varying type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>VARCHAR(&lt;length&gt;)</code>, <code>CHAR VARYING(&lt;length&gt;)</code>,
     * <code>CHARACTER VARYING(&lt;length&gt;)</code>,
     * <code>VARCHAR</code>, <code>CHAR VARYING</code>, <code>CHARACTER VARYING</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getLength()}
     */
    public static final int VARCHAR = 11;

    /**
     * PartiQL's string type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TO_BE_DETERMINED</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int STRING = 12;

    /**
     * SQL's blob type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>BLOB</code>, <code>BLOB(&lt;large object length&gt;)</code>,
     * <code>BINARY LARGE OBJECT</code>, <code>BINARY LARGE OBJECT(&lt;large object length&gt;)</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getLength()}
     */
    public static final int BLOB = 13;

    /**
     * SQL's clob type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>CLOB</code>, <code>CLOB(&lt;large object length&gt;)</code>,
     * <code>CHAR LARGE OBJECT</code>, <code>CHAR LARGE OBJECT(&lt;large object length&gt;)</code>
     * <code>CHARACTER LARGE OBJECT</code>, <code>CHARACTER LARGE OBJECT(&lt;large object length&gt;)</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getLength()}
     */
    public static final int CLOB = 14;

    /**
     * SQL's date type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>DATE</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int DATE = 15;

    /**
     * SQL's time without timezone type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TIME</code>, <code>TIME WITHOUT TIME ZONE</code>,
     * <code>TIME(&lt;precision&gt;)</code>, <code>TIME(&lt;precision&gt;) WITHOUT TIME ZONE</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int TIME = 16;

    /**
     * SQL's time with timezone type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TIME WITH TIME ZONE</code>, <code>TIME(&lt;precision&gt;) WITH TIME ZONE</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int TIMEZ = 17;

    /**
     * SQL's timestamp without timezone type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TIMESTAMP</code>, <code>TIMESTAMP WITHOUT TIME ZONE</code>,
     * <code>TIMESTAMP(&lt;precision&gt;)</code>, <code>TIMESTAMP(&lt;precision&gt;) WITHOUT TIME ZONE</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int TIMESTAMP = 18;

    /**
     * SQL's timestamp with timezone type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>TIMESTAMP WITH TIME ZONE</code>, <code>TIMESTAMP(&lt;precision&gt;) WITH TIME ZONE</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int TIMESTAMPZ = 19;

    /**
     * ARRAY (LIST) represents an ordered collection of elements with type T.
     * <br>
     * <br>
     * <b>Type Syntax</b>
     * <ul>
     *    <li><code>ARRAY</code></li>
     *    <li><code>T ARRAY[N]</code></li>
     *    <li><code>ARRAY{@literal <T>}[N]</code></li>
     * </ul>
     * <br>
     * <br>
     * <b>Equivalences</b>
     * <ol>
     *    <li><code>T ARRAY[N] {@literal <->} ARRAY{@literal <T>}[N]</code></li>
     *    <li><code>ARRAY[N] {@literal <->} DYNAMIC ARRAY[N] {@literal <->} ARRAY{@literal <DYNAMIC>}[N]</code></li>
     *    <li><code>ARRAY {@literal <->} DYNAMIC ARRAY {@literal <->} ARRAY{@literal <DYNAMIC>} {@literal <->} LIST</code></li>
     * </ol>
     * <br>
     * <b>Applicable methods</b>:
     * {@link PType#getTypeParameter()}
     */
    public static final int ARRAY = 20;

    /**
     * BAG represents an unordered collection of elements with type T.
     * <br>
     * <br>
     * <b>Type Syntax</b>
     * <ul>
     *    <li><code>BAG</code></li>
     *    <li><code>T BAG[N]</code></li>
     *    <li><code>BAG{@literal <T>}[N]</code></li>
     * </ul>
     * <br>
     * <br>
     * <b>Equivalences</b>
     * <ol>
     *    <li><code>T BAG[N] {@literal <->} BAG{@literal <T>}[N]</code></li>
     *    <li><code>BAG[N] {@literal <->} DYNAMIC BAG[N] {@literal <->} BAG{@literal <DYNAMIC>}[N]</code></li>
     *    <li><code>BAG {@literal <->} DYNAMIC BAG {@literal <->} BAG{@literal <DYNAMIC>}</code></li>
     * </ol>
     * <br>
     * <b>Applicable methods</b>:
     * {@link PType#getTypeParameter()}
     */
    public static final int BAG = 21;

    /**
     * SQL's row type. Characterized as a closed, ordered collection of fields.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>ROW(&lt;str&gt;: &lt;type&gt;, ...)</code>
     * <br>
     * <b>Applicable methods</b>:
     * {@link PType#getFields()}
     */
    public static final int ROW = 22;

    /**
     * Ion's struct type. Characterized as an open, unordered collection of fields (duplicates allowed).
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>STRUCT</code>
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int STRUCT = 23;

    /**
     * PartiQL's unknown type. This temporarily represents literal null and missing values.
     * <br>
     * <br>
     * <b>Type Syntax</b>: NONE
     * <br>
     * <b>Applicable methods</b>: NONE
     */
    public static final int UNKNOWN = 24;

    /**
     * The variant type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: T VARIANT or VARIANT[T]
     * <br>
     */
    public static final int VARIANT = 25;

    /**
     * <p>
     * The year-month interval types.
     * </p>
     * <p>
     * <b>Applicable Methods:</b>
     * <ul>
     * <li>{@link PType#getIntervalCode()}</li>
     * <li>{@link PType#getPrecision()}</li>
     * <li>{@link PType#getFractionalPrecision()}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Allowable Interval Codes:</b>
     * <ul>
     * <li>{@link IntervalCode#YEAR}</li>
     * <li>{@link IntervalCode#MONTH}</li>
     * <li>{@link IntervalCode#YEAR_MONTH}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Type Syntax:</b>
     * <ul>
     * <li>INTERVAL YEAR (precision)</li>
     * <li>INTERVAL MONTH (precision)</li>
     * <li>INTERVAL YEAR (precision) TO MONTH</li>
     * </ul>
     * </p>
     */
    public static final int INTERVAL_YM = 26;

    /**
     * <p>
     * The date-time interval types.
     * </p>
     * <p>
     * <b>Applicable Methods:</b>
     * <ul>
     * <li>{@link PType#getIntervalCode()}</li>
     * <li>{@link PType#getPrecision()}</li>
     * <li>{@link PType#getFractionalPrecision()}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Allowable Interval Codes:</b>
     * <ul>
     * <li>{@link IntervalCode#DAY}</li>
     * <li>{@link IntervalCode#HOUR}</li>
     * <li>{@link IntervalCode#MINUTE}</li>
     * <li>{@link IntervalCode#SECOND}</li>
     * <li>{@link IntervalCode#DAY_HOUR}</li>
     * <li>{@link IntervalCode#DAY_MINUTE}</li>
     * <li>{@link IntervalCode#DAY_SECOND}</li>
     * <li>{@link IntervalCode#HOUR_MINUTE}</li>
     * <li>{@link IntervalCode#HOUR_SECOND}</li>
     * <li>{@link IntervalCode#MINUTE_SECOND}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Type Syntax:</b>
     * <ul>
     * <li>INTERVAL DAY (precision)</li>
     * <li>INTERVAL HOUR (precision)</li>
     * <li>INTERVAL MINUTE (precision)</li>
     * <li>INTERVAL SECOND (precision, fractionalPrecision)</li>
     * <li>INTERVAL DAY (precision) TO HOUR</li>
     * <li>INTERVAL DAY (precision) TO MINUTE</li>
     * <li>INTERVAL DAY (precision) TO SECOND (fractionalPrecision)</li>
     * <li>INTERVAL HOUR (precision) TO MINUTE</li>
     * <li>INTERVAL HOUR (precision) TO SECOND (fractionalPrecision)</li>
     * <li>INTERVAL MINUTE (precision) TO SECOND (fractionalPrecision)</li>
     * </ul>
     * </p>
     */
    public static final int INTERVAL_DT = 27;

    /**
     * Creates a type representing INTERVAL YEAR (precision)
     * @param precision the interval's leading field precision
     * @return a type representing INTERVAL YEAR (precision)
     */
    @NotNull
    public static PType intervalYear(int precision) {
        return new PTypeIntervalYearMonth(IntervalCode.YEAR, precision);
    }

    /**
     * Creates a type representing INTERVAL MONTH (precision)
     * @param precision the interval's leading field precision
     * @return a type representing INTERVAL MONTH (precision)
     */
    @NotNull
    public static PType intervalMonth(int precision) {
        return new PTypeIntervalYearMonth(IntervalCode.MONTH, precision);
    }

    /**
     * Creates a type representing INTERVAL DAY (precision)
     * @param precision the interval's leading field precision
     * @return a type representing INTERVAL DAY (precision)
     */
    @NotNull
    public static PType intervalDay(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.DAY, precision);
    }

    /**
     * Creates a type representing INTERVAL HOUR (precision)
     * @param precision the interval's leading field precision
     * @return a type representing INTERVAL HOUR (precision)
     */
    @NotNull
    public static PType intervalHour(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.HOUR, precision);
    }

    /**
     * Creates a type representing INTERVAL MINUTE (precision)
     * @param precision the interval's leading field precision
     * @return a type representing INTERVAL MINUTE (precision)
     */
    @NotNull
    public static PType intervalMinute(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.MINUTE, precision);
    }

    /**
     * Creates a type representing INTERVAL SECOND (precision, fractionalPrecision)
     * @param precision the number of decimal digits allowed on the left side of the decimal point
     * @param fractionalPrecision the number of decimal digits allowed on the right side of the decimal point
     * @return a type representing INTERVAL SECOND (precision, fractionalPrecision)
     */
    @NotNull
    public static PType intervalSecond(int precision, int fractionalPrecision) {
        return new PTypeIntervalDateTime(IntervalCode.SECOND, precision, fractionalPrecision);
    }

    /**
     * Creates a type representing INTERVAL YEAR (precision) TO MONTH
     * @param precision the precision as it pertains to YEAR
     * @return a type representing INTERVAL YEAR (precision) TO MONTH
     */
    @NotNull
    public static PType intervalYearMonth(int precision) {
        return new PTypeIntervalYearMonth(IntervalCode.YEAR_MONTH, precision);
    }

    /**
     * Creates a type representing INTERVAL DAY (precision) TO HOUR
     * @param precision the precision as it pertains to DAY
     * @return a type representing INTERVAL DAY (precision) TO HOUR
     */
    @NotNull
    public static PType intervalDayHour(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.DAY_HOUR, precision);
    }

    /**
     * Creates a type representing INTERVAL DAY (precision) TO MINUTE
     * @param precision the number of decimal digits allowed for DAY.
     * @return a type representing INTERVAL DAY (precision) TO MINUTE
     */
    @NotNull
    public static PType intervalDayMinute(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.DAY_MINUTE, precision);
    }

    /**
     * Creates a type representing INTERVAL DAY (precision) TO SECOND (fractionalPrecision)
     * @param precision the number of decimal digits allowed for DAY.
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS.
     * @return a type representing INTERVAL DAY (precision) TO SECOND (fractionalPrecision)
     */
    @NotNull
    public static PType intervalDaySecond(int precision, int fractionalPrecision) {
        return new PTypeIntervalDateTime(IntervalCode.DAY_SECOND, precision, fractionalPrecision);
    }

    /**
     * Creates a type representing INTERVAL HOUR (precision) TO MINUTE
     * @param precision the number of decimal digits allowed for HOUR
     * @return a type representing INTERVAL HOUR (precision) TO MINUTE
     */
    @NotNull
    public static PType intervalHourMinute(int precision) {
        return new PTypeIntervalDateTime(IntervalCode.HOUR_MINUTE, precision);
    }

    /**
     * Creates a type representing INTERVAL HOUR (precision) TO SECOND (fractionalPrecision)
     * @param precision the number of decimal digits allowed for HOUR.
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS.
     * @return a type representing INTERVAL HOUR (precision) TO SECOND (fractionalPrecision)
     */
    @NotNull
    public static PType intervalHourSecond(int precision, int fractionalPrecision) {
        return new PTypeIntervalDateTime(IntervalCode.HOUR_SECOND, precision, fractionalPrecision);
    }

    /**
     * Creates a type representing INTERVAL MINUTE (precision) TO SECOND (fractionalPrecision)
     * @param precision the number of decimal digits allowed for MINUTE.
     * @param fractionalPrecision the number of decimal digits on the right side of the decimal point, for SECONDS.
     * @return a type representing INTERVAL MINUTE (precision) TO SECOND (fractionalPrecision)
     */
    @NotNull
    public static PType intervalMinuteSecond(int precision, int fractionalPrecision) {
        return new PTypeIntervalDateTime(IntervalCode.MINUTE_SECOND, precision, fractionalPrecision);
    }

    /**
     * @return a PartiQL dynamic type
     */
    @NotNull
    public static PType dynamic() {
        return new PTypePrimitive(DYNAMIC);
    }

    /**
     * @return a PartiQL boolean type
     */
    @NotNull
    public static PType bool() {
        return new PTypePrimitive(BOOL);
    }

    /**
     * @return a PartiQL tiny integer type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType tinyint() {
        return new PTypePrimitive(TINYINT);
    }

    /**
     * @return a PartiQL small integer type
     */
    @NotNull
    public static PType smallint() {
        return new PTypePrimitive(SMALLINT);
    }

    /**
     * @return a PartiQL integer type
     */
    @NotNull
    public static PType integer() {
        return new PTypePrimitive(INTEGER);
    }

    /**
     * @return a PartiQL big integer type
     */
    @NotNull
    public static PType bigint() {
        return new PTypePrimitive(BIGINT);
    }

    /**
     * @return a SQL:1999 NUMERIC type.
     */
    @NotNull
    public static PType numeric(int precision, int scale) {
        return new PTypeDecimal(NUMERIC, precision, scale);
    }

    /**
     * @return a SQL:1999 NUMERIC type with default precision (38) and scale (0).
     */
    @NotNull
    public static PType numeric() {
        return new PTypeDecimal(NUMERIC, 38, 0);
    }

    /**
     * @return a PartiQL decimal type
     */
    @NotNull
    public static PType decimal(int precision, int scale) {
        return new PTypeDecimal(PType.DECIMAL, precision, scale);
    }

    /**
     * @return a PartiQL decimal type with default precision (38) and scale (0).
     */
    @NotNull
    public static PType decimal() {
        return new PTypeDecimal(PType.DECIMAL, 38, 0);
    }

    /**
     * @return a PartiQL real type.
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType real() {
        return new PTypePrimitive(REAL);
    }

    /**
     * @return a PartiQL double precision type
     */
    @NotNull
    public static PType doublePrecision() {
        return new PTypePrimitive(DOUBLE);
    }

    /**
     * @return a PartiQL char type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType character(int length) {
        return new PTypeWithMaxLength(CHAR, length);
    }

    /**
     * @return a PartiQL char type with a default length of 1
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType character() {
        return new PTypeWithMaxLength(CHAR, 1);
    }

    /**
     * @return a PartiQL varchar type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType varchar(int length) {
        return new PTypeWithMaxLength(VARCHAR, length);
    }

    /**
     * @return a PartiQL varchar type with a default length of 1
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType varchar() {
        return new PTypeWithMaxLength(VARCHAR, 1);
    }

    /**
     * @return a PartiQL string type
     */
    @NotNull
    public static PType string() {
        return new PTypePrimitive(STRING);
    }

    /**
     * @return a PartiQL clob type
     */
    @NotNull
    @SuppressWarnings("SameParameterValue")
    public static PType clob(int length) {
        return new PTypeWithMaxLength(CLOB, length);
    }

    /**
     * @return a PartiQL clob type with a default length of {@link Integer#MAX_VALUE}.
     */
    @NotNull
    public static PType clob() {
        return new PTypeWithMaxLength(CLOB, Integer.MAX_VALUE);
    }

    /**
     * @return a PartiQL blob type
     */
    @NotNull
    @SuppressWarnings("SameParameterValue")
    public static PType blob(int length) {
        return new PTypeWithMaxLength(BLOB, length);
    }

    /**
     * @return a PartiQL blob type with a default length of {@link Integer#MAX_VALUE}.
     */
    @NotNull
    public static PType blob() {
        return new PTypeWithMaxLength(BLOB, Integer.MAX_VALUE);
    }

    /**
     * @return a PartiQL date type
     */
    @NotNull
    public static PType date() {
        return new PTypePrimitive(DATE);
    }

    /**
     * @return a PartiQL time without timezone type
     */
    @NotNull
    public static PType time(int precision) {
        return new PTypeWithPrecisionOnly(TIME, precision);
    }

    /**
     * @return a PartiQL time without timezone type with a default precision of 6.
     */
    @NotNull
    public static PType time() {
        return new PTypeWithPrecisionOnly(TIME, 6);
    }

    /**
     * @return a PartiQL time with timezone type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType timez(int precision) {
        return new PTypeWithPrecisionOnly(TIMEZ, precision);
    }

    /**
     * @return a PartiQL time with timezone type with a default precision of 6.
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType timez() {
        return new PTypeWithPrecisionOnly(TIMEZ, 6);
    }

    /**
     * @return a PartiQL timestamp without timezone type
     */
    @NotNull
    public static PType timestamp(int precision) {
        return new PTypeWithPrecisionOnly(TIMESTAMP, precision);
    }

    /**
     * @return a PartiQL timestamp without timezone type with a default precision of 6.
     */
    @NotNull
    public static PType timestamp() {
        return new PTypeWithPrecisionOnly(TIMESTAMP, 6);
    }

    /**
     * @return a PartiQL timestamp with timezone type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType timestampz(int precision) {
        return new PTypeWithPrecisionOnly(TIMESTAMPZ, precision);
    }

    /**
     * @return a PartiQL timestamp with timezone type with a default precision of 6.
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType timestampz() {
        return new PTypeWithPrecisionOnly(TIMESTAMPZ, 6);
    }

    /**
     * @return a PartiQL list type with a component type of dynamic
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType array() {
        return new PTypeCollection(ARRAY, PType.dynamic());
    }

    /**
     * @return a PartiQL list type with a component type of {@code typeParam}
     */
    @NotNull
    public static PType array(@NotNull PType typeParam) {
        return new PTypeCollection(ARRAY, typeParam);
    }

    /**
     * @return a PartiQL bag type with a component type of dynamic
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType bag() {
        return new PTypeCollection(BAG, PType.dynamic());
    }

    /**
     * @return a PartiQL bag type with a component type of {@code typeParam}
     */
    @NotNull
    public static PType bag(@NotNull PType typeParam) {
        return new PTypeCollection(BAG, typeParam);
    }

    /**
     * @return a PartiQL row type
     */
    @NotNull
    public static PType row(@NotNull Collection<PTypeField> fields) {
        return new PTypeRow(fields);
    }

    /**
     * @return a PartiQL row type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType row(@NotNull PTypeField... fields) {
        return new PTypeRow(Arrays.asList(fields));
    }

    /**
     * @return a PartiQL struct type
     */
    @NotNull
    public static PType struct() {
        return new PTypePrimitive(STRUCT);
    }

    /**
     * @return a PartiQL unknown type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType unknown() {
        return new PTypePrimitive(UNKNOWN);
    }

    /**
     * @param encoding variant encoding type.
     * @return a PartiQL variant type.
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType variant(String encoding) {
        return new PTypeVariant(encoding);
    }

    /**
     * @param code PartiQL type code
     * @return the PartiQL type corresponding with the code; if the type is parameterized, then the returned type will
     * contain the default parameters
     * @throws IllegalArgumentException if the code is not recognized
     */
    @NotNull
    public static PType of(int code) throws IllegalArgumentException {
        switch (code) {
            case DYNAMIC:
                return dynamic();
            case BOOL:
                return bool();
            case TINYINT:
                return tinyint();
            case SMALLINT:
                return smallint();
            case INTEGER:
                return integer();
            case BIGINT:
                return bigint();
            case NUMERIC:
                return numeric();
            case DECIMAL:
                return decimal();
            case REAL:
                return real();
            case DOUBLE:
                return doublePrecision();
            case CHAR:
                return character();
            case VARCHAR:
                return varchar();
            case STRING:
                return string();
            case CLOB:
                return clob();
            case BLOB:
                return blob();
            case DATE:
                return date();
            case TIME:
                return time();
            case TIMEZ:
                return timez();
            case TIMESTAMP:
                return timestamp();
            case TIMESTAMPZ:
                return timestampz();
            case ARRAY:
                return array();
            case BAG:
                return bag();
            case ROW:
                return row();
            case STRUCT:
                return struct();
            case UNKNOWN:
                return unknown();
            case VARIANT:
                return variant("ion");
            default:
                throw new IllegalArgumentException("Unknown type code: " + code);
        }
    }
}
