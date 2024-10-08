package org.partiql.spi.errors;

/**
 * TODO
 */
public class Property {

    /**
     * Do not allow for construction of a property key.
     */
    private Property() {}

    /**
     * This is used to maintain forwards-compatibility of this enum.
     */
    public static final int UNKNOWN = 0;

    /**
     * The line number where the error originated from.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.Integer}. The value returned may change
     * without prior notice.
     */
    public static final int LINE_NO = 1;

    /**
     * The column number where the error originated from.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.Integer}. The value returned may change
     * without prior notice.
     */
    public static final int COLUMN_NO = 2;

    /**
     * The length of the offending token.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.Integer}. The value returned may change
     * without prior notice.
     */
    public static final int LENGTH = 3;

    /**
     * The token's internal name.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.String}. This should not be exposed to end
     * users as it exposes the underlying (internal) grammar's rule name. Therefore, the name of the token may be changed
     * without prior notice.
     */
    public static final int TOKEN_NAME = 4;

    /**
     * The token's content.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.String}.
     */
    public static final int TOKEN_CONTENT = 5;

    /**
     * In the case of {@link ErrorCode#UNEXPECTED_TOKEN}, this property represents the expected tokens.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.util.Set} of {@link java.lang.String}. The value
     * returned may change without prior notice.
     * @deprecated INTERNAL NOTE: IS THIS NEEDED?
     */
    public static final int EXPECTED_TOKENS = 6;

    /**
     * The (internal) grammar rule name of the error resulting from {@link ErrorCode#UNEXPECTED_TOKEN}.
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.String}. This is for debugging purposes and
     * should not be exposed to end users. The value returned may change without prior notice.
     */
    public static final int RULE = 7;

    /**
     * The cause of the {@link Error} with {@link ErrorCode#INTERNAL_ERROR}. This represents the JVM simple name.
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.String}. This is for debugging purposes and
     * should not be exposed to end users. The value returned may change without prior notice.
     */
    public static final int CAUSE = 8;

    /**
     * The name of the feature that caused the {@link Error} with {@link ErrorCode#FEATURE_NOT_SUPPORTED}.
     * Result of {@link Error#getProperty(int)} shall be a {@link java.lang.String}. This is for debugging purposes and
     * should not be exposed to end users. The value returned may change without prior notice.
     */
    public static final int FEATURE_NAME = 9;

    /**
     * The type of the input that caused the {@link Error} with {@link ErrorCode#UNDEFINED_CAST}.
     * Result of {@link Error#getProperty(int)} shall be a {@link org.partiql.types.PType}. The value returned may change
     * without prior notice.
     */
    public static final int INPUT_TYPE = 10;

    /**
     * The type of the target that caused the {@link Error} with {@link ErrorCode#UNDEFINED_CAST}.
     * Result of {@link Error#getProperty(int)} shall be a {@link org.partiql.types.PType}. The value returned may change
     * without prior notice.
     */
    public static final int TARGET_TYPE = 11;

    /**
     * The stack trace of the error. This shall be used for debugging purposes only.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be an array of {@link java.lang.StackTraceElement}. The value
     * returned may change without prior notice.
     */
    public static final int STACK_TRACE = 12;

    /**
     * The identifier chain associated with the {@link Error}
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link org.partiql.spi.catalog.Identifier>}. The value returned
     * may change without prior notice.
     */
    public static final int IDENTIFIER_CHAIN = 13;

    /**
     * The input argument types of the function call.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.util.List} of
     * {@link org.partiql.types.PType}. The value returned may change without prior notice.
     */
    public static final int INPUT_ARGUMENT_TYPES = 14;

    /**
     * The potential candidates of the function call.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.util.List} of
     * {@link org.partiql.spi.function.Function}. The value returned may change without prior notice.
     */
    public static final int FN_VARIANTS = 15;

    /**
     * The allowed types.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.util.Set} of
     * {@link org.partiql.types.PType}. The value returned may change without prior notice.
     */
    public static final int ALLOWED_TYPES = 16;

    /**
     * The local variables.
     * <br><br>
     * Result of {@link Error#getProperty(int)} shall be a {@link java.util.Set} of
     * {@link java.lang.String}. The value returned may change without prior notice.
     */
    public static final int LOCALS = 17;
}
