package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class Literal extends AstEnum {
    public static final int UNKNOWN = 0;
    // absent literals
    public static final int NULL = 1;
    public static final int MISSING = 2;
    // boolean literal
    public static final int BOOL = 3;
    // numeric literals
    public static final int APPROX_NUM = 4;
    public static final int EXACT_NUM = 5;
    public static final int INT_NUM = 6;
    // string literal
    public static final int STRING = 7;
    // typed string literal
    public static final int TYPED_STRING = 8;

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
            default: return "UNKNOWN";
        }
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
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

    // Value extraction
    /**
     * TODO docs
     * Valid for just BOOL
     */
    public boolean booleanValue() {
        if (code == BOOL) {
            requireNonNull(boolValue, "bool value");
            return boolValue;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * TODO docs
     * Valid for just APPROX_NUM, EXACT_NUM, and INT_NUM.
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
     * TODO docs
     * Valid for just EXACT_NUM and INT_NUM
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
     * TODO docs
     * Valid for just STRING and TYPED_STRING
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
     * TODO docs
     * Valid for just TYPED_STRING
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
