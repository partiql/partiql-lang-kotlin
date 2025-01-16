package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.Enum;
import org.partiql.spi.SourceLocation;
import org.partiql.spi.UnsupportedCodeException;
import org.partiql.spi.catalog.Identifier;
import org.partiql.spi.function.Function;
import org.partiql.spi.types.PType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Represents an error/warning in the PartiQL ecosystem.
 * <br><br>
 * Errors consist of an error code and a collection of (nullable) properties. The error code can be found by invoking
 * {@link PError#code()}. Before attempting to invoke any of the get methods
 * (i.e. {@link PError#getOrNull(String, Class)}, etc.), please read the error code's Javadocs to see what properties
 * are potentially available.
 * <br><br>
 * All error codes are defined as static final integers in {@link PError}.
 * <br><br>
 * <b>WARNING</b>: The available properties are subject to change without prior notice. The property's values are also
 * subject to change without prior notice. Your application <b>MUST</b> be able to handle these scenarios.
 * @see PError#code()
 * @see PError#kind
 * @see PError#severity
 * @see PError#getOrNull(String, Class)
 * @see PError#getListOrNull(String, Class)
 * @see PErrorListener
 * @see PErrorListenerException
 */
public final class PError extends Enum {
    // NOTE: This is named PError to not be confused with java.lang.Error

    /**
     * The classification associated with this error.
     */
    @NotNull
    public PErrorKind kind;

    /**
     * The severity associated with this error.
     */
    @NotNull
    public Severity severity;

    /**
     * The (potentially absent) location of this error in the user's input.
     */
    @Nullable
    public SourceLocation location;

    @NotNull
    private final Map<String, Object> properties;

    ///
    ///
    /// CONSTRUCTORS
    ///
    ///

    /**
     * To be used by the public static methods.
     * @param code see {@link PError#code()}
     * @param severity see {@link PError#severity}
     * @param kind see {@link PError#kind}
     * @param location see {@link PError#location}
     * @param properties the set of available properties. These should match the properties identified in
     * each {@link PError#code()}.
     */
    public PError(
            int code,
            @NotNull Severity severity,
            @NotNull PErrorKind kind,
            @Nullable SourceLocation location,
            @Nullable Map<String, Object> properties
    ) {
        super(code);
        this.kind = kind;
        this.severity = severity;
        this.location = location;
        if (properties != null) {
            this.properties = properties;
        } else {
            this.properties = new HashMap<>();
        }
    }

    ///
    ///
    /// PUBLIC METHODS
    ///
    ///

    /**
     * Returns the error's property with the corresponding {@code key}.
     * Users should read the documentation for each error code to determine the potentially applicable properties
     * that may be present in this error as well as the expected type of the property.
     * <br>
     * <b>NOTE</b>: It is recommended to use {@link PError#getOrNull(String, Class)} instead.
     * @param key the key of the property to retrieve. See the error code's documentation for available properties.
     * @param clazz the class that this will be cast to.
     * @param <T> the expected type of the property.
     * @return a property that may provide additional information about the error; {@code null} if the property does not exist.
     * @throws ClassCastException when the property's value cannot be cast to {@code T}.
     */
    @Nullable
    public <T> T get(@NotNull String key, @NotNull Class<T> clazz) throws ClassCastException {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    /**
     * Users should read the documentation for each error code to determine the potentially applicable properties
     * that may be present in this error as well as the expected type of the property.
     * <br>
     * <b>NOTE</b>: It is recommended to use {@link PError#getListOrNull(String, Class)} instead.
     * @param key the key of the property to retrieve. See the error code's documentation for available properties.
     * @param clazz the expected type of the property's elements. See the Javadocs for the expected property's types.
     * @return a list containing {@code T}; {@code null} if the property does not exist.
     * @param <T> the expected type of the property's elements.
     * @throws ClassCastException when the property's value is not a list, or if any of the list's elements cannot be
     * cast to {@code clazz}.
     */
    @Nullable
    public <T> List<T> getList(@NotNull String key, @NotNull Class<T> clazz) throws ClassCastException {
        List<?> values = get(key, List.class);
        if (values == null) {
            return null;
        }
        return values.stream().map(clazz::cast).collect(toList());
    }

    /**
     * This is a type-safe way to get a property that is expected to be a {@link List} whose elements are of type
     * {@code clazz}.
     * @param key the key of the property to retrieve. See the error code's documentation for available properties.
     * @param clazz the expected type of the property's elements. See the Javadocs for the expected property's types.
     * @return a list containing {@code T}; {@code null} if the property does not exist, or if a property cannot be
     * cast to a {@link List} of {@code clazz}.
     * @param <T> the expected type of the property's elements.
     */
    @Nullable
    public <T> List<T> getListOrNull(@NotNull String key, @NotNull Class<T> clazz) {
        try {
            return getList(key, clazz);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    /**
     * Returns the error's property with the corresponding {@code key}.
     * Users should read the documentation for each error code to determine the potentially applicable properties
     * that may be present in this error as well as the expected type of the property.
     * @param key the key of the property to retrieve. See the error code's documentation for available properties.
     * @param clazz the class that this will be cast to.
     * @param <T> the expected type of the property.
     * @return a property that may provide additional information about the error; {@code null} if the property does
     * not exist, or if the property cannot be cast to {@code clazz}.
     */
    public <T> T getOrNull(@NotNull String key, @NotNull Class<T> clazz) {
        try {
            return get(key, clazz);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    @NotNull
    @Override
    public String name() throws UnsupportedCodeException {
        int code = code();
        switch (code) {
            case INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            case UNRECOGNIZED_TOKEN:
                return "UNRECOGNIZED_TOKEN";
            case UNEXPECTED_TOKEN:
                return "UNEXPECTED_TOKEN";
            case PATH_KEY_NEVER_SUCCEEDS:
                return "PATH_KEY_NEVER_SUCCEEDS";
            case PATH_SYMBOL_NEVER_SUCCEEDS:
                return "PATH_SYMBOL_NEVER_SUCCEEDS";
            case PATH_INDEX_NEVER_SUCCEEDS:
                return "PATH_INDEX_NEVER_SUCCEEDS";
            case FEATURE_NOT_SUPPORTED:
                return "FEATURE_NOT_SUPPORTED";
            case UNDEFINED_CAST:
                return "UNDEFINED_CAST";
            case FUNCTION_NOT_FOUND:
                return "FUNCTION_NOT_FOUND";
            case FUNCTION_TYPE_MISMATCH:
                return "FUNCTION_TYPE_MISMATCH";
            case VAR_REF_NOT_FOUND:
                return "VAR_REF_NOT_FOUND";
            case VAR_REF_AMBIGUOUS:
                return "VAR_REF_AMBIGUOUS";
            case TYPE_UNEXPECTED:
                return "TYPE_UNEXPECTED";
            case ALWAYS_MISSING:
                return "ALWAYS_MISSING";
            default:
                throw new UnsupportedCodeException(code);
        }
    }

    /**
     * The value returned may change without prior notice. Consumers of this method should not depend on this.
     * @return a string representation of an error, for debugging purposes only.
     */
    @Override
    public String toString() {
        String name;
        try {
            name = name();
        } catch (UnsupportedCodeException e) {
            name = String.valueOf(code());
        }
        return "PError{" +
                "code=" + name +
                ", severity=" + severity +
                ", kind=" + kind +
                ", location=" + location +
                ", properties=" + properties +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PError)) return false;
        PError pError = (PError) o;
        return Objects.equals(kind, pError.kind) && Objects.equals(severity, pError.severity) && Objects.equals(location, pError.location) && Objects.equals(properties, pError.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, severity, location, properties);
    }

    //
    //
    // PUBLIC STATIC METHODS
    //
    //

    /**
     * @param kind see {@link PError#kind}
     * @param location see {@link PError#location}
     * @param cause see {@link PError#INTERNAL_ERROR}
     * @return an error representing {@link PError#INTERNAL_ERROR}
     */
    public static PError INTERNAL_ERROR(@NotNull PErrorKind kind, @Nullable SourceLocation location, @Nullable Throwable cause) {
        // Since this is used across multiple components, this is exposed as a static method as an easy-to-use API.
        return new PError(
                INTERNAL_ERROR,
                Severity.ERROR(),
                kind,
                location,
                new HashMap<String, Object>() {{
                    put("CAUSE", cause);
                }}
        );
    }

    //
    //
    // PUBLIC STATIC FIELDS AND METHODS
    //
    //

    /**
     * An internal error occurred during the evaluation of this expression. This error code is non-recoverable and
     * indicates a bug in the implementation of this library. The properties exposed by this error are for debugging
     * purposes only and should not be rendered to external customers.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>CAUSE</b> ({@link Throwable}): The cause of the internal error.</li>
     * </ul>
     * <br>
     * Example error message: <code>Internal error occurred.</code>
     */
    public static final int INTERNAL_ERROR = 1;

    /**
     * This is a lexing error, where a token was unable to be produced by the available input.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>CONTENT</b> ({@link String}): The unrecognized token's content.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Could not tokenize input: "[token_content]"</code>
     */
    public static final int UNRECOGNIZED_TOKEN = 2;

    /**
     * This is a parsing error, where a token was unexpected.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>TOKEN_NAME</b> ({@link String}): The token's name in the grammar.</li>
     * <li><b>EXPECTED_TOKENS</b> ({@link List} of {@link String}): The expected token's names.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Unexpected token: [name]. Expected to find one of: [expected].</code>
     */
    public static final int UNEXPECTED_TOKEN = 3;

    /**
     * This is a semantic warning, where the path-key ({@code some_struct."k"} or {@code some_struct['k']}) expression
     * is statically known to always return the missing value. In strict
     * mode, this will result in a failure. In permissive mode, excessive computation may occur if this warning is
     * left unheeded.
     * <br>
     * Example error message: <code>[location]: Pathing expression always returns missing.</code>
     */
    public static final int PATH_KEY_NEVER_SUCCEEDS = 4;

    /**
     * This is a semantic warning, where the path-symbol ({@code some_struct.k}) expression is statically known to
     * always return the missing value. In strict
     * mode, this will result in a failure. In permissive mode, excessive computation may occur if this warning is
     * left unheeded.
     * <br>
     * Example error message: <code>[location]: Pathing expression always returns missing.</code>
     */
    public static final int PATH_SYMBOL_NEVER_SUCCEEDS = 5;

    /**
     * This is a semantic warning, where the path-index ({@code some_array[5]}) expression is statically known to
     * always return the missing value. In strict
     * mode, this will result in a failure. In permissive mode, excessive computation may occur if this warning is
     * left unheeded.
     * <br>
     * Example error message: <code>[location]: Index expression always returns missing.</code>
     */
    public static final int PATH_INDEX_NEVER_SUCCEEDS = 6;

    /**
     * This is an error, where some aspect of the user's input is not supported.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>FEATURE_NAME</b> ({@link String}): The name of the feature that is not yet supported.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: This database implementation does not yet support: [feature_name]</code>
     */
    public static final int FEATURE_NOT_SUPPORTED = 7;

    /**
     * This is a semantic warning or runtime error, where the input expression cannot be cast to the specified type.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>INPUT_TYPE</b> ({@link PType}): The input type of the cast.</li>
     * <li><b>TARGET_TYPE</b> ({@link PType}): The target type of the cast.</li>
     * </ul>
     * <br>
     * Example error message:
     * <code>[location]: Cannot cast input ([input_type]) to type: [target_type]</code>
     */
    public static final int UNDEFINED_CAST = 8;

    /**
     * This is a semantic warning or runtime error, where a function invocation does not have any potential candidates
     * (no functions with matching name and same number of parameters).
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>FN_ID</b> ({@link Identifier}): Represents the user-specified identifier of the function invocation.</li>
     * <li><b>ARG_TYPES</b> ({@link List} of {@link PType}): Represents the user-specified identifier of the function invocation.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Function not found: [fn_id]([arg_types]).</code>
     */
    public static final int FUNCTION_NOT_FOUND = 9;

    /**
     * This is a semantic warning or runtime error, where a function invocation cannot resolve to a candidate function
     * due to the invocation's arguments not matching any of the available candidate's parameters.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>FN_ID</b> ({@link Identifier}): Represents the user-specified identifier of the function invocation.</li>
     * <li><b>ARG_TYPES</b> ({@link List} of {@link PType}): Types of the arguments.</li>
     * <li><b>CANDIDATES</b> ({@link List} of {@link Function}): The candidate functions</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Function reference ([fn_id]([arg_types])) cannot resolve to one of: [candidates].</code>
     */
    public static final int FUNCTION_TYPE_MISMATCH = 10;

    /**
     * This is a semantic error, where the input expression refers to a variable that does not exist.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>ID</b> ({@link Identifier}): Represents the user-specified identifier that could not be resolved.</li>
     * <li><b>LOCALS</b> ({@link List} of {@link String}): These are locally-defined variables that the user has access to.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Could not resolve [id] in the database environment or in the set of available locals: [locals].</code>
     */
    public static final int VAR_REF_NOT_FOUND = 11;

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
     * <li><b>ID</b> ({@link Identifier}): Represents the user-specified identifier was ambiguous.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Ambiguous reference: [id]</code>
     */
    public static final int VAR_REF_AMBIGUOUS = 12;

    /**
     * This is a semantic error, where the type of an expression does not match what was expected.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>EXPECTED_TYPES</b> ({@link List} of {@link PType}): The allowable types of the expression.</li>
     * <li><b>ACTUAL_TYPE</b> ({@link PType}): The actual type of the expression.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Unexpected type encountered: [actual_type]. Allowed types: [expected_types].</code>
     */
    public static final int TYPE_UNEXPECTED = 13;

    /**
     * This is a semantic warning, where an expression is statically known to
     * always return the missing value. For example: {@code 1 + MISSING}.
     * <br>
     * Example error message: <code>[location]: Expression always returns missing.</code>
     */
    public static final int ALWAYS_MISSING = 14;

    /**
     * This is a semantic warning, where an exclude path is statically known to
     * not exist. For example: {@code SELECT * EXCLUDE some_struct."k" FROM << { 'a': 1 } >> AS some_struct }
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>PATH</b> ({@link String}): A string representing the invalid path.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Expression always returns null.</code>
     */
    public static final int INVALID_EXCLUDE_PATH = 15;

    /**
     * <p>
     * This is a semantic/evaluation error, where a cardinality violation occurs.
     * </p>
     * <p>
     * From SQL:1999:
     * > If the cardinality of a {@code <row subquery>} is greater than 1 (one), then an exception condition is raised: cardinality violation.
     * > If the cardinality of SS {@code <scalar subquery>} is greater than 1 (one), then an exception condition is raised: cardinality violation.
     * </p>
     * <p>
     * Example error message: <code>[location]: Cardinality violation.</code>
     * </p>
     */
    public static final int CARDINALITY_VIOLATION = 16;

    /**
     * <p>
     * This is a data exception, where a numeric value is out of range.
     * </p>
     * <p>
     * Potentially available properties:
     * <ul>
     * <li><b>VALUE</b> ({@link String}): The value/operation (in string form) that is out of range.</li>
     * <li><b>TYPE</b> ({@link PType}): The type that could not fit the value.</li>
     * </ul>
     * </p>
     * <p>
     * Example error message: <code>[location]: Numeric value out of range for [type]: [value].</code>
     * </p>
     */
    public static final int NUMERIC_VALUE_OUT_OF_RANGE = 17;

    /**
     * <p>
     * This is a data exception, where the string value exceeds the maximum length. This is characterized in SQL:1999 by
     * {@code data exception — string data, right truncation}.
     * </p>
     * <p>
     * Potentially available properties:
     * <ul>
     * <li><b>LENGTH</b> ({@link Long}): The length of the value that does not fit in the type.</li>
     * <li><b>TYPE</b> ({@link PType}): The type that could not fit the value.</li>
     * </ul>
     * </p>
     * <p>
     * Example error message: <code>[location]: Character string with length [length] exceeds maximum length for [type].</code>
     * </p>
     */
    public static final int STRING_EXCEEDS_LENGTH = 18;
}
