package org.partiql.types;

import org.jetbrains.annotations.NotNull;

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
public abstract class PType extends Enum {

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
    public Collection<Field> getFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The decimal precision of the type
     *
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
        };
    }

    @Override
    public @NotNull String name() {
        switch (code()) {
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
            case PType.INTERVAL_DT:
                return "INTERVAL_DT";
            case PType.INTERVAL_YM:
                return "INTERVAL_YM";
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
            default:
                return "UNKNOWN";
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
     * The SQL year-month interval type.
     */
    public static final int INTERVAL_YM = 26;

    /**
     * The SQL day-time interval type.
     * <br>
     * <br>
     * <b>Type Syntax</b>: <code>INTERVAL DAY TO SECOND</code>, <code>INTERVAL DAY TO SECOND(&lt;precision&gt;)</code>
     * <br>
     * <b>Applicable methods</b>: {@link PType#getPrecision()}
     */
    public static final int INTERVAL_DT = 27;

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
     * @return a PartiQL decimal type
     */
    @NotNull
    public static PType decimal(int precision, int scale) {
        return new PTypeDecimal(PType.DECIMAL, precision, scale);
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
     * @return a PartiQL char type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType varchar(int length) {
        return new PTypeWithMaxLength(VARCHAR, length);
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
     * @return a PartiQL blob type
     */
    @NotNull
    @SuppressWarnings("SameParameterValue")
    public static PType blob(int length) {
        return new PTypeWithMaxLength(BLOB, length);
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
     * @return a PartiQL time with timezone type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType timez(int precision) {
        return new PTypeWithPrecisionOnly(TIMEZ, precision);
    }

    /**
     * @return a PartiQL timestamp without timezone type
     */
    @NotNull
    public static PType timestamp(int precision) {
        return new PTypeWithPrecisionOnly(TIMESTAMP, precision);
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
     * @return a PartiQL interval year-month type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType intervalYM() {
        return new PTypePrimitive(INTERVAL_YM);
    }

    /**
     * @param precision fractional seconds precision
     * @return a PartiQL interval day-time type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType intervalDT(int precision) {
        return new PTypeWithPrecisionOnly(INTERVAL_DT, precision);
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
    public static PType row(@NotNull Collection<Field> fields) {
        return new PTypeRow(fields);
    }

    /**
     * @return a PartiQL row type
     */
    @NotNull
    @SuppressWarnings("unused")
    public static PType row(@NotNull Field... fields) {
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
}
