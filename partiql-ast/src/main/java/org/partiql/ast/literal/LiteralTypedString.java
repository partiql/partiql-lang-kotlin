package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.DataType;

@EqualsAndHashCode(callSuper = false)
class LiteralTypedString extends Literal {
    @NotNull
    private final DataType type;

    @NotNull
    private final String value;

    LiteralTypedString(@NotNull DataType type, @NotNull String value) {
        this.type = type;
        this.value = value;
    }

    @NotNull
    @Override
    public DataType dataType() {
        return type;
    }

    @NotNull
    @Override
    public String stringValue() {
        return value;
    }

    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.TYPED_STRING();
    }
}
