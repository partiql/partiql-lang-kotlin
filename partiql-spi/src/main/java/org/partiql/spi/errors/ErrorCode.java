package org.partiql.spi.errors;

public class ErrorCode {
    // This makes sure customers never instantiate this.
    private ErrorCode() {}

    /**
     * This is a mechanism to allow for forward-compatibility of this API. If a later version of PartiQL sends a new
     * error code to this particular version of the library, users of this library are enabled to leverage this variant
     * of the error code.
     */
    public static final int UNKNOWN = 0;

    /**
     * <p>
     * An internal error occurred during the evaluation of this expression. This error code is non-recoverable and
     * indicates a bug in the implementation of this library.
     * </p>
     * <p>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#CAUSE}</li>
     * </ul>
     * </p>
     */
    public static final int INTERNAL_ERROR = 1;

    /**
     * <p>
     * This is a lexing error, where a token was unable to be produced by the available input.
     * </p>
     * <p>
     * Potentially available properties:
     * <ul>
     * <li>{@link org.partiql.errors.Property#TOKEN_STRING}</li>
     * <li>{@link org.partiql.errors.Property#LINE_NUMBER}</li>
     * <li>{@link org.partiql.errors.Property#COLUMN_NUMBER}</li>
     * </ul>
     * </p>
     * <p>
     * A good error message to end users may look like: <code>[line]:[column]:[length] Could not tokenize input: "[token_content]"</code>
     * </p>
     */
    public static final int UNRECOGNIZED_TOKEN = 2;

    /**
     * <p>
     * This is a parsing error, where a token was unexpected.
     * </p>
     * <p>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#TOKEN_NAME}</li>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * </ul>
     * </p>
     * <p>
     * A good error message to end users may look like: <code>[line]:[column]:[length] Unexpected token: [token_name]. Expected to find one of: [expected_tokens].</code>
     * </p>
     */
    public static final int UNEXPECTED_TOKEN = 3;

    public static final int ALWAYS_MISSING = 4;
}
