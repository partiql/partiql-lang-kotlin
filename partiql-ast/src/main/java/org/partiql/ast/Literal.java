package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A literal value, such as a number, string, or boolean.
 */
@EqualsAndHashCode(callSuper = false)
public final class Literal extends AstEnum {
    // absent literals
    /**
     * Null literal.
     */
    public static final int NULL = 0;
    /**
     * Missing literal.
     */
    public static final int MISSING = 1;
    // boolean literal
    /**
     * Boolean literal such as {@code true} or {@code false}.
     */
    public static final int BOOL = 2;
    // numeric literals
    /**
     * Approximate numeric literal such as {@code 1.0e1}.
     */
    public static final int APPROX_NUM = 3;
    /**
     * Exact numeric literal such as {@code 1.2345}.
     */
    public static final int EXACT_NUM = 4;
    /**
     * Integer numeric literal such as {@code 123}.
     */
    public static final int INT_NUM = 5;
    // string literal
    /**
     * Single-quoted string literal such as {@code 'foo'}.
     */
    public static final int STRING = 6;
    // typed string literal
    /**
     * Typed string literal such as {@code DATE '2025-01-01'}.
     */
    public static final int TYPED_STRING = 7;

    // Literal fields
    private final int code;
    private final Boolean boolValue;
    private final String stringValue;
    private final DataType dataType;

    /////// Constructors
    private Literal(int code, Boolean value, String stringValue, DataType dataType) {
        this.code = code;
        this.boolValue = value;
        this.stringValue = stringValue;
        this.dataType = dataType;
    }

    // Private constructor for absent literals
    private Literal(int code) {
        this.code = code;
        // Rest set to null
        this.boolValue = null;
        this.stringValue = null;
        this.dataType = null;
    }

    // Private constructor for boolean literal
    private Literal(boolean value) {
        this.code = BOOL;
        this.boolValue = value;
        // Rest set to null
        this.stringValue = null;
        this.dataType = null;
    }

    // Private constructor for literals stored w/ just a string (e.g. numerics, single-quoted strings)
    private Literal(int code, String value) {
        this.code = code;
        this.stringValue = value;
        // Rest set to null
        this.boolValue = null;
        this.dataType = null;
    }

    // Private constructor for typed string literal
    private Literal(DataType dataType, String value) {
        this.code = TYPED_STRING;
        this.stringValue = value;
        this.dataType = dataType;
        // Rest set to null
        this.boolValue = null;
    }

    @Override
    public int code() {
        return code;
    }

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case NULL: return "NULL";
            case MISSING: return "MISSING";
            case BOOL: return "BOOL";
            case APPROX_NUM: return "APPROX_NUM";
            case EXACT_NUM: return "EXACT_NUM";
            case INT_NUM: return "INT_NUM";
            case STRING: return "STRING";
            case TYPED_STRING: return "TYPED_STRING";
            default: throw new IllegalStateException("Invalid Literal code: " + code);
        }
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitLiteral(this, ctx);
    }

    // Factory methods
    @NotNull
    public static Literal approxNum(@NotNull String value) {
        return new Literal(APPROX_NUM, value);
    }

    @NotNull
    public static Literal bool(boolean value) {
        return new Literal(value);
    }

    @NotNull
    public static Literal exactNum(@NotNull BigDecimal value) {
        if (value.scale() == 0) {
            return new Literal(EXACT_NUM, value + ".");
        } else {
            return new Literal(EXACT_NUM, value.toString());
        }
    }

    @NotNull
    public static Literal exactNum(@NotNull String value) {
        return new Literal(EXACT_NUM, value);
    }

    @NotNull
    public static Literal intNum(int value) {
        return new Literal(INT_NUM, Integer.toString(value));
    }

    @NotNull
    public static Literal intNum(long value) {
        return new Literal(INT_NUM, Long.toString(value));
    }

    @NotNull
    public static Literal intNum(@NotNull BigInteger value) {
        return new Literal(INT_NUM, value.toString());
    }

    @NotNull
    public static Literal intNum(@NotNull String value) {
        return new Literal(INT_NUM, value);
    }

    @NotNull
    public static Literal nul() {
        return new Literal(NULL);
    }

    @NotNull
    public static Literal missing() {
        return new Literal(MISSING);
    }

    @NotNull
    public static Literal string(@NotNull String value) {
        return new Literal(STRING, value);
    }

    @NotNull
    public static Literal typedString(@NotNull DataType type, @NotNull String value) {
        return new Literal(type, value);
    }

    /**
     * @return the boolean value of this literal.
     * @throws UnsupportedOperationException if this literal does not have code {@link Literal#BOOL}.
     */
    // Value extraction
    public boolean booleanValue() {
        if (code == BOOL) {
            requireNonNull(boolValue, "bool value");
            return boolValue;
        }
        throw new UnsupportedOperationException();
    }


    /**
     * @return the number value of this literal.
     * @throws UnsupportedOperationException if this literal does not have code {@link Literal#APPROX_NUM}, {@link Literal#EXACT_NUM}, or {@link Literal#INT_NUM}.
     */
    @NotNull
    public String numberValue() {
        switch (code) {
            case APPROX_NUM:
            case EXACT_NUM:
            case INT_NUM:
                requireNonNull(stringValue, "string value for numerics");
                return stringValue;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * @return the {@link BigDecimal} value of this literal.
     * @throws UnsupportedOperationException if this literal does not have code {@link Literal#EXACT_NUM} or {@link Literal#INT_NUM}.
     */
    @NotNull
    public BigDecimal bigDecimalValue() {
        switch (code) {
            case EXACT_NUM:
            case INT_NUM:
                requireNonNull(stringValue, "string value for exact and int numerics");
                return new BigDecimal(stringValue);
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * @return the string value of this literal.
     * @throws UnsupportedOperationException if this literal does not have code {@link Literal#STRING} or {@link Literal#TYPED_STRING}.
     */
    @NotNull
    public String stringValue() {
        switch (code) {
            case STRING:
            case TYPED_STRING:
                requireNonNull(stringValue, "string value");
                return stringValue;
            default:
                throw new UnsupportedOperationException();
        }
    }


    /**
     * @return the associated data type for this literal.
     * @throws UnsupportedOperationException if this literal does not have code {@link Literal#TYPED_STRING}.
     */
    @NotNull
    public DataType dataType() {
        if (code == TYPED_STRING) {
            requireNonNull(dataType, "data type");
            return dataType;
        }
        throw new UnsupportedOperationException();
    }
}
