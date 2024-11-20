package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralDouble extends Literal {
    public double value;

    @NotNull
    public String text;

    public LiteralDouble(@NotNull String text) {
        this.text = text;
        this.value = Double.parseDouble(text);
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }
}
