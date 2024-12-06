package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
class LiteralMissing extends Literal {
    @NotNull
    @Override
    public LiteralKind kind() {
        return LiteralKind.MISSING();
    }
}
