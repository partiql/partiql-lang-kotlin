package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralNull extends Literal {
    public LiteralNull() {
        super("NULL");
    }
}
