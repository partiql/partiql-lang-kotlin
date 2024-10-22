package org.partiql.eval.compiler;

import org.partiql.plan.Operator;

public class Match {

    private final Operator[] operands;

    public Match(Operator operator) {
        this.operands = new Operator[] { operator };
    }

    public Match(Operator[] operands) {
        this.operands = operands;
    }

    public Operator get(int i) {
        return operands[i];
    }
}
