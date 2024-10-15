package org.partiql.eval.operator;

import org.partiql.plan.Operator;

import java.util.function.Predicate;

/**
 * Strategy converts a logical operator into a physical operator. The compiler uses the list of operands
 * to determine a subtree match, then invokes `apply` to
 */
public abstract class Strategy {

    private final Operand operand;

    protected Strategy(Operand operand) {
        this.operand = operand;
    }

    /**
     * Applies the strategy to a logical plan operator and returns the physical operation.
     *
     * @param operator the logical operator to be converted
     * @return the physical operation
     */
    public abstract PhysicalOperator apply(Operator operator);

    /**
     * Create an operand that matches the given class.
     */
    public static Operand operand(Class<? extends Operator> clazz) {
        return new Operand(clazz, (Operator o) -> true);
    }

    /**
     * Create an operand that matches the given class and predicate.
     */
    public static Operand operand(Class<? extends Operator> clazz, Predicate<Operator> predicate) {
        return new Operand(clazz, predicate);
    }

    /**
     * Represents an operand for a strategy.
     */
    public static class Operand {

        private final Class<? extends Operator> clazz;
        private final Predicate<Operator> predicate;

        public Operand(Class<? extends Operator> clazz, Predicate<Operator> predicate) {
            this.clazz = clazz;
            this.predicate = predicate;
        }

        public boolean matches(Operator operator) {
            if (!clazz.isInstance(operator)) {
                return false;
            }
            return predicate.test(operator);
        }
    }
}
