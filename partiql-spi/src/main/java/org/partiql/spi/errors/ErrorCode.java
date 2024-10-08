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
     * An internal error occurred during the evaluation of this expression. This error code is non-recoverable and
     * indicates a bug in the implementation of this library.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#CAUSE}</li>
     * <li>{@link Property#STACK_TRACE}</li>
     * </ul>
     * <br>
     * Example error message: <code>Internal error: [cause]</code>
     */
    public static final int INTERNAL_ERROR = 1;

    /**
     * This is a lexing error, where a token was unable to be produced by the available input.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link org.partiql.errors.Property#TOKEN_STRING}</li>
     * <li>{@link org.partiql.errors.Property#LINE_NUMBER}</li>
     * <li>{@link org.partiql.errors.Property#COLUMN_NUMBER}</li>
     * </ul>
     * <br>
     * A good error message to end users may look like: <code>[line]:[column]:[length] Could not tokenize input: "[token_content]"</code>
     */
    public static final int UNRECOGNIZED_TOKEN = 2;

    /**
     * This is a parsing error, where a token was unexpected.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#TOKEN_NAME}</li>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Unexpected token: [token_name]. Expected to find one of: [expected_tokens].</code>
     */
    public static final int UNEXPECTED_TOKEN = 3;

    /**
     * This is a semantic warning, where the input expression will always return the missing value. In strict
     * mode, this will result in a failure. In permissive mode, excessive computation may occur if this warning is
     * left unheeded.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Expression always returns missing.</code>
     */
    public static final int ALWAYS_MISSING = 4;

    /**
     * This is a semantic error, where the input expression is not supported by this implementation.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#FEATURE_NAME}</li>
     * </ul>
     * <br>
     * Example error message: <code>Feature not supported: [feature_name]</code>
     */
    public static final int FEATURE_NOT_SUPPORTED = 5;

    /**
     * This is a semantic error, where the input expression cannot be cast to the specified type.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#INPUT_TYPE}</li>
     * <li>{@link Property#TARGET_TYPE}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Cannot cast input ([input_type]) to type: [target_type]</code>
     */
    public static final int UNDEFINED_CAST = 6;

    /**
     * This is a semantic error, where the input expression refers to an undefined function.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * <li>{@link Property#IDENTIFIER_CHAIN}: Represents the name of the function.</li>
     * <li>{@link Property#FN_VARIANTS}: Represents the candidates of the function call.</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Undefined function: [function_name]. Available functions: [fn_variants].</code>
     */
    public static final int UNDEFINED_FUNCTION = 7;

    /**
     * This is a semantic error, where the input expression refers to an undefined variable.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * <li>{@link Property#IDENTIFIER_CHAIN}: Represents the name of the variable.</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Undefined variable: [identifier_chain].</code>
     */
    public static final int UNDEFINED_VARIABLE = 8;

    /**
     * This is an execution error, where the input expression has an invalid limit value. This corresponds with
     * SQL:1999 Error 22-020
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Invalid limit value.</code>
     */
    public static final int INVALID_LIMIT_VALUE = 9;

    /**
     * This is an execution error, where the input expression has an invalid offset value.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Invalid offset value.</code>
     */
    public static final int INVALID_OFFSET_VALUE = 10;

    /**
     * This is a semantic error, where the input has a type mismatch.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * <li>{@link Property#INPUT_TYPE}</li>
     * <li>{@link Property#TARGET_TYPE}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Cannot cast input ([input_type]) to type: [target_type]</code>
     */
    public static final int TYPE_MISMATCH = 11;

    /**
     * This is a semantic error, where a variable reference is ambiguous. For example:
     * <br>
     * {@code SELECT name FROM orders o JOIN customers c ON o.customer_id = c.id;}
     * <br>
     * Above, {@code name} is ambiguous because it could refer to either the {@code name} column in the {@code orders}
     * table or the {@code name} column in the {@code customers} table.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li>{@link Property#LINE_NO}</li>
     * <li>{@link Property#COLUMN_NO}</li>
     * <li>{@link Property#LENGTH}</li>
     * <li>{@link Property#IDENTIFIER_CHAIN}</li>
     * </ul>
     * <br>
     * Example error message: <code>[line]:[column]:[length] Ambiguous reference: [identifier_chain]</code>
     */
    public static final int AMBIGUOUS_REF = 12;
}
