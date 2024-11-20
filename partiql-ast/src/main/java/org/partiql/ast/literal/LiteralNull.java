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
    @Override
    public String getText() {
        return "NULL";
    }
}
