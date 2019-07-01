package org.partiql.testscript.evaluator;

import org.partiql.testscript.compiler.TestScriptExpression;

import java.util.List;

/**
 * A PTS Evaluator that takes in a list of {@code TestScriptExpression} and evaluates them into a list of
 * {@code TestResult}.
 */
public abstract class Evaluator {
    private final PtsEquality equality;

    protected Evaluator(PtsEquality equality) {
        this.equality = equality;
    }

    public PtsEquality getEquality() {
        return equality;
    }

    public abstract List<TestResult> evaluate(final List<TestScriptExpression> testExpressions);
}

