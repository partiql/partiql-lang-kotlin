package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralLong extends Literal {
    public long value;

    public LiteralLong(long value) {
        super(String.format("%d", value));
        this.value = value;
    }
}
