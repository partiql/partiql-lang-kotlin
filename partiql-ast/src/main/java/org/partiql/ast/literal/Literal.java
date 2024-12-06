package org.partiql.ast.literal;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;

public abstract class Literal extends AstNode {
    @NotNull
    public abstract LiteralKind kind();

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
    public static Literal litApprox(@NotNull String value) {
        return new LiteralApprox(value);
    }

    public static Literal litBool(boolean value) {
        return new LiteralBool(value);
    }

    public static Literal litExact(@NotNull BigDecimal value) {
        if (value.scale() == 0) {
            return new LiteralExact(value + ".");
        } else {
            return new LiteralExact(value.toString());
        }
    }

    public static Literal litExact(@NotNull String value) {
        return new LiteralExact(value);
    }

    public static Literal litInt(int value) {
        return new LiteralInt(Integer.toString(value));
    }

    public static Literal litInt(long value) {
        return new LiteralInt(Long.toString(value));
    }

    public static Literal litInt(@NotNull BigInteger value) {
        return new LiteralInt(value.toString());
    }

    public static Literal litInt(@NotNull String value) {
        return new LiteralInt(value);
    }

    public static Literal litNull() {
        return new LiteralNull();
    }

    public static Literal litMissing() {
        return new LiteralMissing();
    }

    public static Literal litString(@NotNull String value) {
        return new LiteralString(value);
    }

    public static Literal litTypedString(@NotNull DataType type, @NotNull String value) {
        return new LiteralTypedString(type, value);
    }

    // Value extraction
    /**
     * TODO docs
     * Valid for just LiteralBool
     */
    public boolean booleanValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO docs
     * Valid for just LiteralApprox, LiteralInt, and LiteralExact
     */
    @NotNull
    public String numberValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO docs
     * Valid for just LiteralInt and LiteralExact
     */
    @NotNull
    public BigDecimal bigDecimalValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO docs
     * Valid for just LiteralString and LiteralTypedString
     */
    @NotNull
    public String stringValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO docs
     * Valid for just LiteralTypedString
     */
    @NotNull
    public DataType dataType() {
        throw new UnsupportedOperationException();
    }
}
