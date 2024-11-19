package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralInt extends Literal {
    public int value;

    public LiteralInt(int value) {
        super(String.format("%d", value));
        this.value = value;
    }
}
