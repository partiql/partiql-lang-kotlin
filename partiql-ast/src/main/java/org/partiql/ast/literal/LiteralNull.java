package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralNull extends Literal {
    public LiteralNull() {}

    @NotNull
    public static LiteralNull litNull() {
        return new LiteralNull();
    }

    @NotNull
    @Override
    public String getText() {
        return "NULL";
    }
}
