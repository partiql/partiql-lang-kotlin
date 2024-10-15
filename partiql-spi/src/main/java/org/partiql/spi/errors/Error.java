package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.SourceLocation;
import org.partiql.spi.catalog.Identifier;
import org.partiql.spi.function.Function;
import org.partiql.types.PType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Represents an error/warning in the PartiQL ecosystem.
 * <br><br>
 * Errors consist of an error code and a collection of (nullable) properties.
 * <br><br>
 * <b>WARNING</b>: The available properties are subject to change without prior notice. The property's values are also
 * subject to change without prior notice. Your application <b>MUST</b> be able to handle these scenarios.
 * @see Error#code
 * @see Error#getOrNull(String, Class)
 * @see Error#getListOrNull(String, Class)
 */
public class Error {

    /**
     * The error code associated with this error. Before attempting to invoke any of the get methods
     * (i.e. {@link Error#getOrNull(String, Class)}, etc.), please read the error code's Javadocs to see what properties
     * are potentially available.
     * <br>
     * All error codes are defined as static final integers in {@link Error}.
     */
    public int code;

    /**
     * TODO
     */
    public int classification;

    /**
     * The (potentially absent) location of this error in the user's input.
     */
    @Nullable
    public SourceLocation location;

    @Override
    public String toString() {
        return "Error{" +
                "code=" + code +
                ", classification=" + classification +
                ", location=" + location +
                ", properties=" + properties +
                '}';
    }

    ///
    ///
    /// PRIVATE CONSTRUCTORS
    ///
    ///

    @NotNull
    private final Map<String, Object> properties;

    // To not allow for creation of an error outside the static methods.
    @SuppressWarnings("unused")
    private Error() {
        code = INTERNAL_ERROR;
        properties = new HashMap<>();
        location = null;
    }

    /**
     * To be used by the public static methods.
     */
    private Error(int code, @Nullable SourceLocation location) {
        this.code = code;
        this.location = location;
        this.properties = new HashMap<>();
    }

    /**
     * To be used by the public static methods.
     */
    private Error(int code, @Nullable SourceLocation location, @NotNull Map<String, Object> properties) {
        this.code = code;
        this.location = location;
        this.properties = properties;
    }

    /**
     * To be used by the public static methods.
     */
    private Error(int code, @Nullable SourceLocation location, @NotNull String key1, @Nullable Object val1) {
        this(
                code,
                location,
                new HashMap<String, Object>() {{
                    put(key1, val1);
                }}
        );
    }

    /**
     * To be used by the public static methods.
     */
    private Error(int code, @Nullable SourceLocation location, @NotNull String key1, @Nullable Object val1, @NotNull String key2, @Nullable Object val2) {
        this(
                code,
                location,
                new HashMap<String, Object>() {{
                    put(key1, val1);
                    put(key2, val2);
                }}
        );
    }

    /**
     * To be used by the public static methods.
     */
    private Error(int code, @Nullable SourceLocation location, @NotNull String key1, @Nullable Object val1, @NotNull String key2, @Nullable Object val2, @NotNull String key3, @Nullable Object val3) {
        this(
                code,
                location,
                new HashMap<String, Object>() {{
                    put(key1, val1);
                    put(key2, val2);
                    put(key3, val3);
                }}
        );
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
     * @param key the key of the property to retrieve. See the error code's documentation for available properties.
     * @param clazz the class that this will be cast to.
     * @param <T> TODO
     * @return a property that may provide additional information about the error; null if the property does not exist.
     * @throws ClassCastException TODO
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
     * TODO
     * @param key TODO
     * @param clazz TODO
     * @return TODO
     * @param <T> TODO
     * @throws ClassCastException TODO
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
     * TODO
     * @param key TODO
     * @param clazz TODO
     * @return TODO
     * @param <T> TODO
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
     * TODO
     * @param key TODO
     * @param clazz TODO
     * @return TODO
     * @param <T> TODO
     */
    public <T> T getOrNull(@NotNull String key, @NotNull Class<T> clazz) {
        try {
            return get(key, clazz);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    //
    //
    // PUBLIC STATIC FIELDS AND METHODS
    //
    //

    /**
     * This is a mechanism to allow for forward-compatibility of this API. If a later version of PartiQL sends a new
     * error code to this particular version of the library, users of this library are enabled to leverage this variant
     * of the error code.
     */
    public static final int UNKNOWN = 0;

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
     * @param location see {@link Error#location}
     * @param cause see {@link Error#INTERNAL_ERROR}
     * @return an error representing {@link Error#INTERNAL_ERROR}
     */
    public static Error INTERNAL_ERROR(@Nullable SourceLocation location, @Nullable Throwable cause) {
        return new Error(INTERNAL_ERROR, location, "CAUSE", cause);
    }

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
     * @param location see {@link Error#location}
     * @param content see {@link Error#UNRECOGNIZED_TOKEN}
     * @return an error representing {@link Error#UNRECOGNIZED_TOKEN}
     */
    public static Error UNRECOGNIZED_TOKEN(@Nullable SourceLocation location, @Nullable String content) {
        return new Error(UNRECOGNIZED_TOKEN, location, "CONTENT", content);
    }

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
     * @param location see {@link Error#location}
     * @param tokenName  see {@link Error#UNEXPECTED_TOKEN}
     * @param expectedTokens  see {@link Error#UNEXPECTED_TOKEN}
     * @return an error representing {@link Error#UNEXPECTED_TOKEN}
     */
    // TODO: Add the token text
    // TODO: Do we want the offending rule?
    public static Error UNEXPECTED_TOKEN(@Nullable SourceLocation location, @Nullable String tokenName, @Nullable List<String> expectedTokens) {
        return new Error(UNEXPECTED_TOKEN, location, "TOKEN_NAME", tokenName, "EXPECTED_TOKENS", expectedTokens);
    }

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
     * @param location see {@link Error#location}
     * @return an error representing {@link Error#PATH_KEY_NEVER_SUCCEEDS}
     */
    public static Error PATH_KEY_NEVER_SUCCEEDS(@Nullable SourceLocation location) {
        return new Error(PATH_KEY_NEVER_SUCCEEDS, location);
    }

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
     * @param location see {@link Error#location}
     * @return an error representing {@link Error#PATH_SYMBOL_NEVER_SUCCEEDS}
     */
    public static Error PATH_SYMBOL_NEVER_SUCCEEDS(@Nullable SourceLocation location) {
        return new Error(PATH_SYMBOL_NEVER_SUCCEEDS, location);
    }

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
     * @param location see {@link Error#location}
     * @return an error representing {@link Error#PATH_INDEX_NEVER_SUCCEEDS}
     */
    public static Error PATH_INDEX_NEVER_SUCCEEDS(@Nullable SourceLocation location) {
        return new Error(PATH_INDEX_NEVER_SUCCEEDS, location);
    }

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
     * @param location see {@link Error#location}
     * @param featureName  see {@link Error#FEATURE_NOT_SUPPORTED}
     * @return an error representing {@link Error#FEATURE_NOT_SUPPORTED}
     */
    public static Error FEATURE_NOT_SUPPORTED(@Nullable SourceLocation location, @Nullable String featureName) {
        return new Error(FEATURE_NOT_SUPPORTED, location, "FEATURE_NAME", featureName);
    }

    /**
     * This is a semantic warning or runtime error, where the input expression cannot be cast to the specified type.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>INPUT_TYPE</b> ({@link org.partiql.types.PType}): The input type of the cast.</li>
     * <li><b>TARGET_TYPE</b> ({@link org.partiql.types.PType}): The target type of the cast.</li>
     * </ul>
     * <br>
     * Example error message:
     * <code>[location]: Cannot cast input ([input_type]) to type: [target_type]</code>
     */
    public static final int UNDEFINED_CAST = 8;

    /**
     * @param location see {@link Error#location}
     * @param inputType  see {@link Error#UNDEFINED_CAST}
     * @param targetType  see {@link Error#UNDEFINED_CAST}
     * @return an error representing {@link Error#UNDEFINED_CAST}
     */
    public static Error UNDEFINED_CAST(@Nullable SourceLocation location, @Nullable PType inputType, @Nullable PType targetType) {
        return new Error(UNDEFINED_CAST, location, "INPUT_TYPE", inputType, "TARGET_TYPE", targetType);
    }

    /**
     * This is a semantic warning or runtime error, where a function invocation does not have any potential candidates
     * (no functions with matching name and same number of parameters).
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>FN_ID</b> ({@link org.partiql.spi.catalog.Identifier}): Represents the user-specified identifier of the function invocation.</li>
     * <li><b>ARG_TYPES</b> ({@link org.partiql.spi.catalog.Identifier}): Represents the user-specified identifier of the function invocation.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Function not found: [fn_id]([arg_types]).</code>
     */
    public static final int FUNCTION_NOT_FOUND = 9;

    /**
     * @param location see {@link Error#location}
     * @param fnId see {@link Error#FUNCTION_NOT_FOUND}
     * @param argTypes see {@link Error#FUNCTION_NOT_FOUND}
     * @return an error representing {@link Error#FUNCTION_NOT_FOUND}
     */
    public static Error FUNCTION_NOT_FOUND(@Nullable SourceLocation location, @Nullable Identifier fnId, @Nullable List<PType> argTypes) {
        return new Error(FUNCTION_NOT_FOUND, location, "FN_ID", fnId, "ARG_TYPES", argTypes);
    }

    /**
     * This is a semantic warning or runtime error, where a function invocation cannot resolve to a candidate function
     * due to the invocation's arguments not matching any of the available candidate's parameters.
     * <br><br>
     * Potentially available properties:
     * <ul>
     * <li><b>FN_ID</b> ({@link org.partiql.spi.catalog.Identifier}): Represents the user-specified identifier of the function invocation.</li>
     * <li><b>ARG_TYPES</b> ({@link List} of {@link PType}): Types of the arguments.</li>
     * <li><b>CANDIDATES</b> ({@link List} of {@link Function}): The candidate functions</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Function reference ([fn_id]([arg_types])) cannot resolve to one of: [candidates].</code>
     */
    public static final int FUNCTION_TYPE_MISMATCH = 10;

    /**
     * @param location see {@link Error#location}
     * @param fnId see {@link Error#FUNCTION_TYPE_MISMATCH}
     * @param argTypes see {@link Error#FUNCTION_TYPE_MISMATCH}
     * @return an error representing {@link Error#FUNCTION_TYPE_MISMATCH}
     */
    public static Error FUNCTION_TYPE_MISMATCH(@Nullable SourceLocation location, @Nullable Identifier fnId, @Nullable List<PType> argTypes, @Nullable List<Function> candidates) {
        return new Error(FUNCTION_TYPE_MISMATCH, location, "FN_ID", fnId, "ARG_TYPES", argTypes, "CANDIDATES", candidates);
    }

    /**
     * This is a semantic error, where the input expression refers to a variable that does not exist.
     * <br>
     * Potentially available properties:
     * <ul>
     * <li><b>ID</b> ({@link org.partiql.spi.catalog.Identifier}): Represents the user-specified identifier that could not be resolved.</li>
     * <li><b>LOCALS</b> ({@link List} of {@link String}): These are locally-defined variables that the user has access to.</li>
     * </ul>
     * <br>
     * Example error message: <code>[location]: Could not resolve [id] in the database environment or in the set of available locals: [locals].</code>
     */
    public static final int VAR_REF_NOT_FOUND = 11;

    /**
     * @param location see {@link Error#location}
     * @param id see {@link Error#VAR_REF_NOT_FOUND}
     * @param locals see {@link Error#VAR_REF_NOT_FOUND}
     * @return an error representing {@link Error#VAR_REF_NOT_FOUND}
     */
    public static Error VAR_REF_NOT_FOUND(@Nullable SourceLocation location, @Nullable Identifier id, @Nullable List<String> locals) {
        return new Error(VAR_REF_NOT_FOUND, location, "ID", id, "LOCALS", locals);
    }

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
     * <li><b>ID</b> ({@link org.partiql.spi.catalog.Identifier}): Represents the user-specified identifier was ambiguous.</li>
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
     * @param location see {@link Error#location}
     * @param actualType see {@link Error#TYPE_UNEXPECTED}
     * @param expectedTypes see {@link Error#TYPE_UNEXPECTED}
     * @return an error representing {@link Error#TYPE_UNEXPECTED}
     */
    public static Error TYPE_UNEXPECTED(@Nullable SourceLocation location, @Nullable PType actualType, @Nullable List<PType> expectedTypes) {
        return new Error(TYPE_UNEXPECTED, location, "ACTUAL_TYPE", actualType, "EXPECTED_TYPES", expectedTypes);
    }

    /**
     * This is a semantic warning, where an expression is statically known to
     * always return the missing value. For example: {@code 1 + MISSING}.
     * <br>
     * Example error message: <code>[location]: Expression always returns missing.</code>
     */
    public static final int ALWAYS_MISSING = 14;

    /**
     * @param location see {@link Error#location}
     * @return an error representing {@link Error#ALWAYS_MISSING}
     */
    public static Error ALWAYS_MISSING(@Nullable SourceLocation location) {
        return new Error(ALWAYS_MISSING, location);
    }
}
