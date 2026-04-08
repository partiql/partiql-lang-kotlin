package org.partiql.spi.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jetbrains.annotations.NotNull;
import org.partiql.spi.UnsupportedCodeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    protected PType(int code, Map<String, Object> metas) {
        super(code);
        this.metas = metas;
    }

    /**
     * Additional information associated with a {@link PType}.
     * Note: This is experimental and subject to change without prior notice!
     */
    public Map<String, Object> metas = new HashMap<>();

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
     * SQL's numeric type.
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

    private static final String UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH";
    private static final String UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION";
    private static final String UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE";

    private static void setUnspecifiedLengthMeta(PType pType) {
        pType.metas.put(UNSPECIFIED_LENGTH, true);
    }

    private static void setUnspecifiedPrecisionMeta(PType pType) {
        pType.metas.put(UNSPECIFIED_PRECISION, true);
    }

    private static void setUnspecifiedScaleMeta(PType pType) {
        pType.metas.put(UNSPECIFIED_SCALE, true);
    }

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
        PType numeric = new PTypeDecimal(NUMERIC, 38, 0);
        setUnspecifiedPrecisionMeta(numeric);
        setUnspecifiedScaleMeta(numeric);
        return numeric;
    }

    /**
     * @return a SQL:1999 NUMERIC type with provided precision and default scale 0.
     */
    @NotNull
    public static PType numeric(int precision) {
        PType numeric = new PTypeDecimal(NUMERIC, precision, 0);
        setUnspecifiedScaleMeta(numeric);
        return numeric;
    }

    /**
     * @return a PartiQL decimal type
     */
    @NotNull
    public static PType decimal(int precision, int scale) {
        return new PTypeDecimal(PType.DECIMAL, precision, scale);
    }

    /**
     * @return a PartiQL decimal type with provided precision and default scale 0.
     */
    @NotNull
    public static PType decimal(int precision) {
        PType decimal = new PTypeDecimal(PType.DECIMAL, precision, 0);
        setUnspecifiedScaleMeta(decimal);
        return decimal;
    }

    /**
     * @return a PartiQL decimal type with default precision (38) and scale (0).
     */
    @NotNull
    public static PType decimal() {
        PType decimal = new PTypeDecimal(PType.DECIMAL, 38, 0);
        setUnspecifiedPrecisionMeta(decimal);
        setUnspecifiedScaleMeta(decimal);
        return decimal;
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
        PType character = new PTypeWithMaxLength(CHAR, 1);
        setUnspecifiedLengthMeta(character);
        return character;
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
        PType varchar = new PTypeWithMaxLength(VARCHAR, 1);
        setUnspecifiedLengthMeta(varchar);
        return varchar;
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
        PType clob = new PTypeWithMaxLength(CLOB, Integer.MAX_VALUE);
        setUnspecifiedLengthMeta(clob);
        return clob;
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
        PType blob = new PTypeWithMaxLength(BLOB, Integer.MAX_VALUE);
        setUnspecifiedLengthMeta(blob);
        return blob;
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
        PType time = new PTypeWithPrecisionOnly(TIME, 6);
        setUnspecifiedPrecisionMeta(time);
        return time;
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
        PType timez = new PTypeWithPrecisionOnly(TIMEZ, 6);
        setUnspecifiedPrecisionMeta(timez);
        return timez;
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
        PType timestamp = new PTypeWithPrecisionOnly(TIMESTAMP, 6);
        setUnspecifiedPrecisionMeta(timestamp);
        return timestamp;
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
        PType timestampz = new PTypeWithPrecisionOnly(TIMESTAMPZ, 6);
        setUnspecifiedPrecisionMeta(timestampz);
        return timestampz;
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

    // -----------------------------------------------
    // JSON serialization / deserialization
    // -----------------------------------------------

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Serializes this {@link PType} to a JSON string.
     *
     * @return a JSON string representation
     * @throws JsonProcessingException if serialization fails
     */
    @NotNull
    public String toJson() throws JsonProcessingException {
        Map<String, Object> node = toJsonNode(this, null);
        return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    /**
     * Deserializes a {@link PType} from a JSON string.
     *
     * @param json the JSON string
     * @return the deserialized PType
     * @throws JsonProcessingException if deserialization fails
     */
    @NotNull
    public static PType fromJson(@NotNull String json) throws JsonProcessingException {
        JsonNode node = JSON_MAPPER.readTree(json);
        return fromJsonNode(node);
    }

    private static Map<String, Object> toJsonNode(PType p, String fieldNameIfRow) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (fieldNameIfRow != null) {
            out.put("name", fieldNameIfRow);
        }
        String typePlain = p.name();
        out.put("type", typePlain);
        switch (p.code()) {
            case PType.TIME:
            case PType.TIMEZ:
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ: {
                boolean unspecified = isUnspecifiedPrecision(p.metas);
                if (!unspecified) {
                    out.put("precision", p.getPrecision());
                }
                break;
            }
            case PType.DECIMAL:
            case PType.NUMERIC: {
                out.put("precision", p.getPrecision());
                out.put("scale", p.getScale());
                break;
            }
            case PType.CHAR:
            case PType.VARCHAR:
            case PType.CLOB: {
                out.put("length", p.getLength());
                break;
            }
            case PType.BLOB: {
                int len = p.getLength();
                if (len > 0) out.put("length", len);
                break;
            }
            case PType.INTERVAL_YM: {
                out.put("intervalCode", intervalCodeToName(p.getIntervalCode()));
                out.put("precision", p.getPrecision());
                break;
            }
            case PType.INTERVAL_DT: {
                out.put("intervalCode", intervalCodeToName(p.getIntervalCode()));
                out.put("precision", p.getPrecision());
                out.put("fractionalPrecision", p.getFractionalPrecision());
                break;
            }
            default:
                break;
        }
        if (p.metas != null && !p.metas.isEmpty()) {
            out.put("metas", JSON_MAPPER.convertValue(p.metas, Object.class));
        }
        if (p.code() == PType.ROW) {
            List<Map<String, Object>> fields = p.getFields().stream()
                    .map(f -> toJsonNode(f.getType(), f.getName()))
                    .collect(Collectors.toList());
            out.put("fields", fields);
        }
        if (p.code() == PType.ARRAY || p.code() == PType.BAG) {
            out.put("element", toJsonNode(p.getTypeParameter(), null));
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "MethodLength"})
    private static PType fromJsonNode(JsonNode node) {
        if (node == null || node instanceof NullNode) {
            throw new IllegalArgumentException("Null JSON node for PType");
        }
        String typeStr = jsonRequiredText(node, "type").trim();
        Map<String, Object> metas = new LinkedHashMap<>();
        if (node.has("metas") && !node.get("metas").isNull()) {
            metas = JSON_MAPPER.convertValue(node.get("metas"), Map.class);
        }
        PType result;
        switch (typeStr.toUpperCase(Locale.ROOT)) {
            case "ROW": {
                JsonNode fieldsNode = node.get("fields");
                if (fieldsNode == null || !fieldsNode.isArray()) {
                    throw new IllegalArgumentException("ROW requires 'fields'");
                }
                List<PTypeField> fields = new ArrayList<>();
                for (JsonNode f : fieldsNode) {
                    String fname = jsonRequiredText(f, "name");
                    PType ftype = fromJsonNode(f);
                    fields.add(PTypeField.of(fname, ftype));
                }
                result = PType.row(fields);
                break;
            }
            case "ARRAY": {
                JsonNode elemNode = node.get("element");
                if (elemNode == null) throw new IllegalArgumentException("ARRAY requires 'element'");
                result = PType.array(fromJsonNode(elemNode));
                break;
            }
            case "BAG": {
                JsonNode elemNode = node.get("element");
                if (elemNode == null) throw new IllegalArgumentException("BAG requires 'element'");
                result = PType.bag(fromJsonNode(elemNode));
                break;
            }
            case "DECIMAL": {
                int precision = jsonRequiredInt(node, "precision");
                int scale = jsonRequiredInt(node, "scale");
                result = PType.decimal(precision, scale);
                break;
            }
            case "NUMERIC": {
                int precision = jsonRequiredInt(node, "precision");
                int scale = jsonRequiredInt(node, "scale");
                result = PType.numeric(precision, scale);
                break;
            }
            case "CHAR": {
                int len = jsonRequiredInt(node, "length");
                result = PType.character(len);
                break;
            }
            case "VARCHAR": {
                int len = jsonRequiredInt(node, "length");
                result = PType.varchar(len);
                break;
            }
            case "BLOB": {
                if (node.has("length") && !node.get("length").isNull()) {
                    result = PType.blob(node.get("length").intValue());
                } else {
                    result = PType.blob();
                }
                break;
            }
            case "CLOB": {
                int len = jsonRequiredInt(node, "length");
                result = PType.clob(len);
                break;
            }
            case "DYNAMIC":    result = PType.dynamic(); break;
            case "BOOL":
            case "BOOLEAN":    result = PType.bool(); break;
            case "TINYINT":    result = PType.tinyint(); break;
            case "SMALLINT":   result = PType.smallint(); break;
            case "INTEGER":
            case "INT":        result = PType.integer(); break;
            case "BIGINT":     result = PType.bigint(); break;
            case "REAL":       result = PType.real(); break;
            case "DOUBLE":
            case "DOUBLE PRECISION": result = PType.doublePrecision(); break;
            case "STRING":     result = PType.string(); break;
            case "DATE":       result = PType.date(); break;
            case "TIME": {
                if (isUnspecifiedPrecision(metas)) { result = PType.time(); }
                else { result = PType.time(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMEZ": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timez(); }
                else { result = PType.timez(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMESTAMP": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timestamp(); }
                else { result = PType.timestamp(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "TIMESTAMPZ": {
                if (isUnspecifiedPrecision(metas)) { result = PType.timestampz(); }
                else { result = PType.timestampz(jsonRequiredInt(node, "precision")); }
                break;
            }
            case "STRUCT":     result = PType.struct(); break;
            case "UNKNOWN":    result = PType.unknown(); break;
            case "VARIANT":    result = PType.variant("ion"); break;
            case "INTERVAL_YM": {
                int intervalCode = intervalCodeFromName(jsonRequiredText(node, "intervalCode"));
                int precision = jsonRequiredInt(node, "precision");
                switch (intervalCode) {
                    case IntervalCode.YEAR:       result = PType.intervalYear(precision); break;
                    case IntervalCode.MONTH:      result = PType.intervalMonth(precision); break;
                    case IntervalCode.YEAR_MONTH: result = PType.intervalYearMonth(precision); break;
                    default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_YM: " + jsonRequiredText(node, "intervalCode"));
                }
                break;
            }
            case "INTERVAL_DT": {
                int intervalCode = intervalCodeFromName(jsonRequiredText(node, "intervalCode"));
                int precision = jsonRequiredInt(node, "precision");
                int fp = jsonRequiredInt(node, "fractionalPrecision");
                switch (intervalCode) {
                    case IntervalCode.DAY:           result = PType.intervalDay(precision); break;
                    case IntervalCode.HOUR:          result = PType.intervalHour(precision); break;
                    case IntervalCode.MINUTE:        result = PType.intervalMinute(precision); break;
                    case IntervalCode.SECOND:        result = PType.intervalSecond(precision, fp); break;
                    case IntervalCode.DAY_HOUR:      result = PType.intervalDayHour(precision); break;
                    case IntervalCode.DAY_MINUTE:    result = PType.intervalDayMinute(precision); break;
                    case IntervalCode.DAY_SECOND:    result = PType.intervalDaySecond(precision, fp); break;
                    case IntervalCode.HOUR_MINUTE:   result = PType.intervalHourMinute(precision); break;
                    case IntervalCode.HOUR_SECOND:   result = PType.intervalHourSecond(precision, fp); break;
                    case IntervalCode.MINUTE_SECOND: result = PType.intervalMinuteSecond(precision, fp); break;
                    default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_DT: " + jsonRequiredText(node, "intervalCode"));
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported type: " + typeStr);
        }
        if (!metas.isEmpty()) result.metas.putAll(metas);
        return result;
    }

    private static boolean isUnspecifiedPrecision(Map<String, Object> metas) {
        if (metas == null) return false;
        Object v = metas.get(UNSPECIFIED_PRECISION);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private static String jsonRequiredText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).asText();
    }

    private static int jsonRequiredInt(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).intValue();
    }

    private static String intervalCodeToName(int code) {
        switch (code) {
            case IntervalCode.YEAR:          return "YEAR";
            case IntervalCode.MONTH:         return "MONTH";
            case IntervalCode.DAY:           return "DAY";
            case IntervalCode.HOUR:          return "HOUR";
            case IntervalCode.MINUTE:        return "MINUTE";
            case IntervalCode.SECOND:        return "SECOND";
            case IntervalCode.YEAR_MONTH:    return "YEAR_MONTH";
            case IntervalCode.DAY_HOUR:      return "DAY_HOUR";
            case IntervalCode.DAY_MINUTE:    return "DAY_MINUTE";
            case IntervalCode.DAY_SECOND:    return "DAY_SECOND";
            case IntervalCode.HOUR_MINUTE:   return "HOUR_MINUTE";
            case IntervalCode.HOUR_SECOND:   return "HOUR_SECOND";
            case IntervalCode.MINUTE_SECOND: return "MINUTE_SECOND";
            default: throw new IllegalArgumentException("Unknown interval code: " + code);
        }
    }

    private static int intervalCodeFromName(String name) {
        switch (name.toUpperCase(Locale.ROOT)) {
            case "YEAR":          return IntervalCode.YEAR;
            case "MONTH":         return IntervalCode.MONTH;
            case "DAY":           return IntervalCode.DAY;
            case "HOUR":          return IntervalCode.HOUR;
            case "MINUTE":        return IntervalCode.MINUTE;
            case "SECOND":        return IntervalCode.SECOND;
            case "YEAR_MONTH":    return IntervalCode.YEAR_MONTH;
            case "DAY_HOUR":      return IntervalCode.DAY_HOUR;
            case "DAY_MINUTE":    return IntervalCode.DAY_MINUTE;
            case "DAY_SECOND":    return IntervalCode.DAY_SECOND;
            case "HOUR_MINUTE":   return IntervalCode.HOUR_MINUTE;
            case "HOUR_SECOND":   return IntervalCode.HOUR_SECOND;
            case "MINUTE_SECOND": return IntervalCode.MINUTE_SECOND;
            default: throw new IllegalArgumentException("Unknown interval code name: " + name);
        }
    }

    // -----------------------------------------------
    // DDL serialization / deserialization
    // -----------------------------------------------

    /**
     * Serializes this {@link PType} to a DDL type string.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code INTEGER}</li>
     *   <li>{@code DECIMAL(10, 2)}</li>
     *   <li>{@code ARRAY<INTEGER>}</li>
     *   <li>{@code ROW(id INTEGER, name STRING)}</li>
     *   <li>{@code TIMESTAMP(6) WITH TIME ZONE}</li>
     *   <li>{@code INTERVAL YEAR(4) TO MONTH}</li>
     * </ul>
     *
     * @return a DDL type string
     */
    @NotNull
    public String toDDL() {
        return toDDLInternal(this);
    }

    /**
     * Parses a DDL type string into a {@link PType}.
     *
     * @param ddl the DDL type string
     * @return the parsed PType
     * @throws IllegalArgumentException if the DDL string cannot be parsed
     */
    @NotNull
    public static PType fromDDL(@NotNull String ddl) {
        String trimmed = ddl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty DDL type string");
        }
        return parseDDL(trimmed);
    }

    private static String toDDLInternal(PType p) {
        switch (p.code()) {
            case PType.DYNAMIC:    return "DYNAMIC";
            case PType.BOOL:       return "BOOL";
            case PType.TINYINT:    return "TINYINT";
            case PType.SMALLINT:   return "SMALLINT";
            case PType.INTEGER:    return "INTEGER";
            case PType.BIGINT:     return "BIGINT";
            case PType.REAL:       return "REAL";
            case PType.DOUBLE:     return "DOUBLE PRECISION";
            case PType.STRING:     return "STRING";
            case PType.DATE:       return "DATE";
            case PType.STRUCT:     return "STRUCT";
            case PType.UNKNOWN:    return "UNKNOWN";
            case PType.VARIANT:    return "VARIANT";
            case PType.NUMERIC:
                return "NUMERIC(" + p.getPrecision() + ", " + p.getScale() + ")";
            case PType.DECIMAL:
                return "DECIMAL(" + p.getPrecision() + ", " + p.getScale() + ")";
            case PType.CHAR:
                return "CHAR(" + p.getLength() + ")";
            case PType.VARCHAR:
                return "VARCHAR(" + p.getLength() + ")";
            case PType.BLOB:
                return "BLOB(" + p.getLength() + ")";
            case PType.CLOB:
                return "CLOB(" + p.getLength() + ")";
            case PType.TIME:
                return "TIME(" + p.getPrecision() + ")";
            case PType.TIMEZ:
                return "TIME(" + p.getPrecision() + ") WITH TIME ZONE";
            case PType.TIMESTAMP:
                return "TIMESTAMP(" + p.getPrecision() + ")";
            case PType.TIMESTAMPZ:
                return "TIMESTAMP(" + p.getPrecision() + ") WITH TIME ZONE";
            case PType.ARRAY:
                return "ARRAY<" + toDDLInternal(p.getTypeParameter()) + ">";
            case PType.BAG:
                return "BAG<" + toDDLInternal(p.getTypeParameter()) + ">";
            case PType.ROW: {
                StringBuilder sb = new StringBuilder("ROW(");
                boolean first = true;
                for (PTypeField f : p.getFields()) {
                    if (!first) sb.append(", ");
                    sb.append(f.getName()).append(" ").append(toDDLInternal(f.getType()));
                    first = false;
                }
                sb.append(")");
                return sb.toString();
            }
            case PType.INTERVAL_YM:
                return intervalYmToDDL(p.getIntervalCode(), p.getPrecision());
            case PType.INTERVAL_DT:
                return intervalDtToDDL(p.getIntervalCode(), p.getPrecision(), p.getFractionalPrecision());
            default:
                throw new IllegalArgumentException("Cannot convert PType code " + p.code() + " to DDL");
        }
    }

    private static String intervalYmToDDL(int intervalCode, int precision) {
        switch (intervalCode) {
            case IntervalCode.YEAR:       return "INTERVAL YEAR(" + precision + ")";
            case IntervalCode.MONTH:      return "INTERVAL MONTH(" + precision + ")";
            case IntervalCode.YEAR_MONTH: return "INTERVAL YEAR(" + precision + ") TO MONTH";
            default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_YM: " + intervalCode);
        }
    }

    private static String intervalDtToDDL(int intervalCode, int precision, int fractionalPrecision) {
        switch (intervalCode) {
            case IntervalCode.DAY:           return "INTERVAL DAY(" + precision + ")";
            case IntervalCode.HOUR:          return "INTERVAL HOUR(" + precision + ")";
            case IntervalCode.MINUTE:        return "INTERVAL MINUTE(" + precision + ")";
            case IntervalCode.SECOND:        return "INTERVAL SECOND(" + precision + ", " + fractionalPrecision + ")";
            case IntervalCode.DAY_HOUR:      return "INTERVAL DAY(" + precision + ") TO HOUR";
            case IntervalCode.DAY_MINUTE:    return "INTERVAL DAY(" + precision + ") TO MINUTE";
            case IntervalCode.DAY_SECOND:    return "INTERVAL DAY(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            case IntervalCode.HOUR_MINUTE:   return "INTERVAL HOUR(" + precision + ") TO MINUTE";
            case IntervalCode.HOUR_SECOND:   return "INTERVAL HOUR(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            case IntervalCode.MINUTE_SECOND: return "INTERVAL MINUTE(" + precision + ") TO SECOND(" + fractionalPrecision + ")";
            default: throw new IllegalArgumentException("Invalid intervalCode for INTERVAL_DT: " + intervalCode);
        }
    }

    @SuppressWarnings("MethodLength")
    private static PType parseDDL(String ddl) {
        String upper = ddl.toUpperCase(Locale.ROOT).trim();

        // INTERVAL types — must check before simple keyword matching
        if (upper.startsWith("INTERVAL ")) {
            return parseIntervalDDL(ddl.trim());
        }

        // TIMESTAMP(...) WITH TIME ZONE
        if (upper.startsWith("TIMESTAMP") && upper.endsWith("WITH TIME ZONE")) {
            String inner = ddlExtractParens(upper, "TIMESTAMP");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timestampz(Integer.parseInt(inner.trim()));
        }

        // TIMESTAMP(...)
        if (upper.startsWith("TIMESTAMP(")) {
            String inner = ddlExtractParens(upper, "TIMESTAMP");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timestamp(Integer.parseInt(inner.trim()));
        }

        // TIME(...) WITH TIME ZONE
        if (upper.startsWith("TIME") && upper.endsWith("WITH TIME ZONE")) {
            String inner = ddlExtractParens(upper, "TIME");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.timez(Integer.parseInt(inner.trim()));
        }

        // TIME(...)
        if (upper.startsWith("TIME(")) {
            String inner = ddlExtractParens(upper, "TIME");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.time(Integer.parseInt(inner.trim()));
        }

        // DECIMAL(p, s)
        if (upper.startsWith("DECIMAL(")) {
            String inner = ddlExtractParens(upper, "DECIMAL");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            String[] parts = inner.split(",");
            if (parts.length != 2) throw new IllegalArgumentException("DECIMAL requires (precision, scale): " + ddl);
            return PType.decimal(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }

        // NUMERIC(p, s)
        if (upper.startsWith("NUMERIC(")) {
            String inner = ddlExtractParens(upper, "NUMERIC");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            String[] parts = inner.split(",");
            if (parts.length != 2) throw new IllegalArgumentException("NUMERIC requires (precision, scale): " + ddl);
            return PType.numeric(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }

        // CHAR(n)
        if (upper.startsWith("CHAR(")) {
            String inner = ddlExtractParens(upper, "CHAR");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.character(Integer.parseInt(inner.trim()));
        }

        // VARCHAR(n)
        if (upper.startsWith("VARCHAR(")) {
            String inner = ddlExtractParens(upper, "VARCHAR");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.varchar(Integer.parseInt(inner.trim()));
        }

        // BLOB(n)
        if (upper.startsWith("BLOB(")) {
            String inner = ddlExtractParens(upper, "BLOB");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.blob(Integer.parseInt(inner.trim()));
        }

        // CLOB(n)
        if (upper.startsWith("CLOB(")) {
            String inner = ddlExtractParens(upper, "CLOB");
            if (inner == null) throw new IllegalArgumentException("Invalid DDL: " + ddl);
            return PType.clob(Integer.parseInt(inner.trim()));
        }

        // ARRAY<T>
        if (upper.startsWith("ARRAY<") && upper.endsWith(">")) {
            String inner = ddl.trim().substring(6, ddl.trim().length() - 1);
            return PType.array(parseDDL(inner));
        }

        // BAG<T>
        if (upper.startsWith("BAG<") && upper.endsWith(">")) {
            String inner = ddl.trim().substring(4, ddl.trim().length() - 1);
            return PType.bag(parseDDL(inner));
        }

        // ROW(field_name TYPE, ...)
        if (upper.startsWith("ROW(") && upper.endsWith(")")) {
            String inner = ddl.trim().substring(4, ddl.trim().length() - 1);
            List<PTypeField> fields = parseRowFields(inner);
            return PType.row(fields);
        }

        // Simple keyword types
        switch (upper) {
            case "DYNAMIC":          return PType.dynamic();
            case "BOOL":             return PType.bool();
            case "BOOLEAN":          return PType.bool();
            case "TINYINT":          return PType.tinyint();
            case "SMALLINT":         return PType.smallint();
            case "INTEGER":          return PType.integer();
            case "INT":              return PType.integer();
            case "BIGINT":           return PType.bigint();
            case "REAL":             return PType.real();
            case "DOUBLE PRECISION": return PType.doublePrecision();
            case "STRING":           return PType.string();
            case "DATE":             return PType.date();
            case "STRUCT":           return PType.struct();
            case "UNKNOWN":          return PType.unknown();
            case "VARIANT":          return PType.variant("ion");
            case "DECIMAL":          return PType.decimal(38, 0);
            case "NUMERIC":          return PType.numeric(38, 0);
            case "CHAR":             return PType.character(1);
            case "VARCHAR":          return PType.varchar(1);
            case "BLOB":             return PType.blob();
            case "CLOB":             return PType.clob();
            case "TIME":             return PType.time(6);
            case "TIMESTAMP":        return PType.timestamp(6);
            default:
                throw new IllegalArgumentException("Unsupported DDL type: " + ddl);
        }
    }

    /**
     * Extracts the content inside the first set of parentheses after the given prefix.
     * Returns null if no parentheses found.
     */
    private static String ddlExtractParens(String upper, String prefix) {
        int start = upper.indexOf('(', prefix.length());
        if (start < 0) return null;
        // Find matching close paren
        int depth = 1;
        int i = start + 1;
        while (i < upper.length() && depth > 0) {
            if (upper.charAt(i) == '(') depth++;
            else if (upper.charAt(i) == ')') depth--;
            i++;
        }
        if (depth != 0) return null;
        return upper.substring(start + 1, i - 1);
    }

    /**
     * Parses ROW fields from the inner string: "field_name TYPE, field_name TYPE, ..."
     * Handles nested types like ARRAY&lt;ROW(...)&gt; by tracking angle bracket and paren depth.
     */
    private static List<PTypeField> parseRowFields(String inner) {
        List<PTypeField> fields = new ArrayList<>();
        int depth = 0;       // tracks < > and ( )
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<' || c == '(') depth++;
            else if (c == '>' || c == ')') depth--;
            else if (c == ',' && depth == 0) {
                fields.add(parseOneField(inner.substring(start, i).trim()));
                start = i + 1;
            }
        }
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) {
            fields.add(parseOneField(last));
        }
        return fields;
    }

    /**
     * Parses a single ROW field: "field_name TYPE_DDL"
     */
    private static PTypeField parseOneField(String fieldStr) {
        // Split on first whitespace to get field name, rest is the type DDL
        int spaceIdx = fieldStr.indexOf(' ');
        if (spaceIdx < 0) {
            throw new IllegalArgumentException("Invalid ROW field (expected 'name type'): " + fieldStr);
        }
        String name = fieldStr.substring(0, spaceIdx).trim();
        String typeDDL = fieldStr.substring(spaceIdx + 1).trim();
        return PTypeField.of(name, parseDDL(typeDDL));
    }

    private static PType parseIntervalDDL(String ddl) {
        // Remove "INTERVAL " prefix
        String body = ddl.substring("INTERVAL ".length()).trim();
        String upper = body.toUpperCase(Locale.ROOT);

        // Check for "TO" to determine single-field vs range
        int toIdx = findToKeyword(upper);

        if (toIdx < 0) {
            // Single-field interval: YEAR(p), MONTH(p), DAY(p), HOUR(p), MINUTE(p), SECOND(p, fp)
            return parseSingleFieldInterval(body);
        } else {
            // Range interval: e.g., YEAR(p) TO MONTH, DAY(p) TO SECOND(fp)
            String leading = body.substring(0, toIdx).trim();
            String trailing = body.substring(toIdx + 3).trim(); // skip " TO "
            return parseRangeInterval(leading, trailing);
        }
    }

    /**
     * Finds the index of the " TO " keyword that is not inside parentheses.
     */
    private static int findToKeyword(String upper) {
        int depth = 0;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && i + 4 <= upper.length() && upper.substring(i, i + 4).equals(" TO ")) {
                return i;
            }
        }
        return -1;
    }

    private static PType parseSingleFieldInterval(String body) {
        String upper = body.toUpperCase(Locale.ROOT);
        if (upper.startsWith("YEAR(")) {
            int p = Integer.parseInt(ddlExtractParens(upper, "YEAR"));
            return PType.intervalYear(p);
        } else if (upper.startsWith("MONTH(")) {
            int p = Integer.parseInt(ddlExtractParens(upper, "MONTH"));
            return PType.intervalMonth(p);
        } else if (upper.startsWith("DAY(")) {
            int p = Integer.parseInt(ddlExtractParens(upper, "DAY"));
            return PType.intervalDay(p);
        } else if (upper.startsWith("HOUR(")) {
            int p = Integer.parseInt(ddlExtractParens(upper, "HOUR"));
            return PType.intervalHour(p);
        } else if (upper.startsWith("MINUTE(")) {
            int p = Integer.parseInt(ddlExtractParens(upper, "MINUTE"));
            return PType.intervalMinute(p);
        } else if (upper.startsWith("SECOND(")) {
            String inner = ddlExtractParens(upper, "SECOND");
            String[] parts = inner.split(",");
            int p = Integer.parseInt(parts[0].trim());
            int fp = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return PType.intervalSecond(p, fp);
        }
        throw new IllegalArgumentException("Unsupported INTERVAL DDL: INTERVAL " + body);
    }

    private static PType parseRangeInterval(String leading, String trailing) {
        String leadUpper = leading.toUpperCase(Locale.ROOT);
        String trailUpper = trailing.toUpperCase(Locale.ROOT);

        // Extract leading field name and precision
        String leadField;
        int precision;
        int parenIdx = leadUpper.indexOf('(');
        if (parenIdx < 0) {
            throw new IllegalArgumentException("INTERVAL range requires precision on leading field: " + leading);
        }
        leadField = leadUpper.substring(0, parenIdx).trim();
        precision = Integer.parseInt(ddlExtractParens(leadUpper, leadField));

        // Extract trailing field name and optional fractional precision
        String trailField;
        int fractionalPrecision = 0;
        int trailParenIdx = trailUpper.indexOf('(');
        if (trailParenIdx >= 0) {
            trailField = trailUpper.substring(0, trailParenIdx).trim();
            fractionalPrecision = Integer.parseInt(ddlExtractParens(trailUpper, trailField));
        } else {
            trailField = trailUpper.trim();
        }

        String combo = leadField + "_" + trailField;
        switch (combo) {
            case "YEAR_MONTH":    return PType.intervalYearMonth(precision);
            case "DAY_HOUR":      return PType.intervalDayHour(precision);
            case "DAY_MINUTE":    return PType.intervalDayMinute(precision);
            case "DAY_SECOND":    return PType.intervalDaySecond(precision, fractionalPrecision);
            case "HOUR_MINUTE":   return PType.intervalHourMinute(precision);
            case "HOUR_SECOND":   return PType.intervalHourSecond(precision, fractionalPrecision);
            case "MINUTE_SECOND": return PType.intervalMinuteSecond(precision, fractionalPrecision);
            default:
                throw new IllegalArgumentException("Unsupported INTERVAL range: " + leadField + " TO " + trailField);
        }
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
