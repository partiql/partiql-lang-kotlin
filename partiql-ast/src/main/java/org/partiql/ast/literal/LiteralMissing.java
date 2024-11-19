package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class LiteralMissing extends Literal {
    public LiteralMissing() {
        super("MISSING");
    }
}
