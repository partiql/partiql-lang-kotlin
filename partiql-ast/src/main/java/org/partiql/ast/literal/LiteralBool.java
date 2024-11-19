package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralBool extends Literal {
    public boolean value;

    public LiteralBool(boolean value) {
        super(String.valueOf(value));
        this.value = value;
    }
}
