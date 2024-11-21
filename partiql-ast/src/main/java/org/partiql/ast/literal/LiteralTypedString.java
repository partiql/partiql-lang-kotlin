package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.DataType;

/**
 * TODO docs
 * Represent type + 'some string value'
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralTypedString extends Literal {
    @NotNull
    public DataType type;

    @NotNull
    public String value;

    private LiteralTypedString(@NotNull DataType type, @NotNull String value) {
        this.type = type;
        this.value = value;
    }

    @NotNull
    public static LiteralTypedString litTypedString(@NotNull DataType type, @NotNull String value) {
        return new LiteralTypedString(type, value);
    }

    @NotNull
    @Override
    public String getText() {
        return String.format("%s '%s'", type.name(), value);
    }
}
