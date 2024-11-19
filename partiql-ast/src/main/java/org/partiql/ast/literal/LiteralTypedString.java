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

    public LiteralTypedString(@NotNull DataType type, @NotNull String value) {
        super(String.format("%s '%s'", type.name(), value));
        this.type = type;
        this.value = value;
    }
}
