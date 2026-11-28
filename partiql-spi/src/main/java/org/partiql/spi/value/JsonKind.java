package org.partiql.spi.value;

/**
 * The kind of SQL/JSON item held by a value of type {@link org.partiql.spi.types.PType#JSON}, as returned by
 * {@link Datum#getJsonKind()}.
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public final class JsonKind {

    private JsonKind() {}

    /**
     * SQL/JSON null. It is a value, distinct from SQL NULL.
     */
    public static final int NULL = 0;

    /**
     * An SQL/JSON scalar: a string, number, boolean or datetime. See {@link Datum#getJsonScalar()}.
     */
    public static final int SCALAR = 1;

    /**
     * An SQL/JSON array. Its elements are values of type JSON, read with {@link Datum#iterator()}.
     */
    public static final int ARRAY = 2;

    /**
     * An SQL/JSON object. Its members are read with {@link Datum#getFields()}, in order and with repeated keys, and
     * each member's value has type JSON.
     */
    public static final int OBJECT = 3;
}
