package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * Operands in an operator tree used for strategy and rule pattern matching.
 */
public interface Operand extends Iterable<Operator> {

    /**
     * Creates a {@link Single} operand from an input {@link Operator}.
     *
     * @param operator the single operator
     * @return a single operand
     */
    static Operand single(Operator operator) {
        return new Single(operator);
    }

    /**
     * Creates a {@link Variadic} operand from an input list of operands.
     * @return a variadic operand
     *
     * See ImmutableCollections.java ListCopy.
     */
    @SuppressWarnings("unchecked")
    static Operand vararg(List<? extends Operator> operators) {
        return new Variadic((List<Operator>) operators);
    }

    /**
     * A single operator.
     */
    class Single implements Operand {

        @NotNull
        public final Operator operator;

        private Single(@NotNull Operator operator) {
            this.operator = operator;
        }


        @NotNull
        @Override
        public Iterator<Operator> iterator() {
            return List.of(operator).iterator();
        }
    }

    /**
     * A variadic operator.
     */
    class Variadic implements Operand {

        @NotNull
        public final List<Operator> operators;

        private Variadic(@NotNull List<Operator> operators) {
            this.operators = operators;
        }

        @NotNull
        @Override
        public Iterator<Operator> iterator() {
            return operators.iterator();
        }
    }
}
