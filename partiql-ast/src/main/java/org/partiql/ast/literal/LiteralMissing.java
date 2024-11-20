package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralMissing extends Literal {
    public LiteralMissing() {}

    @NotNull
    @Override
    public String getText() {
        return "MISSING";
    }
}
