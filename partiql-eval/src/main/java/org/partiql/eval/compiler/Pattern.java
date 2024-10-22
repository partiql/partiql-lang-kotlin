package org.partiql.eval.compiler;

import org.partiql.plan.Operator;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pattern is like a Calcite RelOptRuleOperand and defines a sub-tree pattern to match against.
 */
public class Pattern {

    private final Class<? extends Operator> clazz;
    private final Predicate<Operator> predicate;
    private final List<Pattern> children;

    protected Pattern(Class<? extends Operator> clazz) {
        this.clazz = clazz;
        this.predicate = (Operator o) -> true;
        this.children = Collections.emptyList();
    }

    protected Pattern(Class<? extends Operator> clazz, Predicate<Operator> predicate, List<Pattern> children) {
        this.clazz = clazz;
        this.predicate = predicate;
        this.children = children;
    }

    public boolean matches(Operator operator) {
        if (!clazz.isInstance(operator)) {
            return false;
        }
        return predicate.test(operator);
    }
}
