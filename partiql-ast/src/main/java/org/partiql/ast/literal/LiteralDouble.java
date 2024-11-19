package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralDouble extends Literal {
    public double value;

    public LiteralDouble(@NotNull String text) {
        super(text);
        this.value = Double.parseDouble(text);
    }
}
