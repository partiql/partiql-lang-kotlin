package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;

/**
 * Match represents a subtree match to be sent to the
 */
public class Match {

    private final Operand[] operands;

    /**
     * Single operand match with zero-or-more inputs.
     *
     * @param operand matched logical operand.
     */
    public Match(@NotNull Operand operand) {
        this.operands = new Operand[]{operand};
    }

    /**
     * Get the first (or only) operand
     *
     * @return Operand
     */
    @NotNull
    public Operand getOperand() {
        return operands[0];
    }
}
