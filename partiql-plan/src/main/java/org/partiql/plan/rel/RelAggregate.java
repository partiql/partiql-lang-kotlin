package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Agg;

import java.util.List;

// TODO GROUP STRATEGY: https://github.com/partiql/partiql-lang-kotlin/issues/1664

/**
 * The logical aggregation abstract base class.
 */
public abstract class RelAggregate extends RelBase {

    /**
     * Creates a new {@link RelAggregate} instance.
     * @param input the input
     * @param measures the measures
     * @param groups the groups
     * @return new {@link RelAggregate} instance
     */
    @NotNull
    public static RelAggregate create(@NotNull Rel input, @NotNull List<Measure> measures, @NotNull List<Rex> groups) {
        return new Impl(input, measures, groups);
    }

    /**
     * Creates a new {@link Measure} instance.
     * @param agg the aggregation function
     * @param args the arguments
     * @param distinct the distinct flag
     * @return new {@link Measure} instance
     */
    @NotNull
    public static Measure measure(@NotNull Agg agg, @NotNull List<Rex> args, boolean distinct) {
        return new Measure(agg, args, distinct);
    }

    /**
     * Gets the input.
     * @return the input (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Gets the measures.
     * @return the measures (arg)
     */
    @NotNull
    public abstract List<Measure> getMeasures();

    /**
     * Gets the groups.
     * @return the groups (arg)
     */
    @NotNull
    public abstract List<Rex> getGroups();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitAggregate(this, ctx);
    }

    /**
     * @return copy with new input.
     */
    @NotNull
    public abstract RelAggregate copy(@NotNull Rel input);

    /**
     * @return copy with new input and args.
     */
    @NotNull
    public abstract RelAggregate copy(@NotNull Rel input, @NotNull List<Measure> measures, @NotNull List<Rex> groups);

    /**
     * An aggregation function along with its arguments and any additional filters (e.g. DISTINCT).
     */
    public static class Measure {

        private final Agg agg;
        private final List<Rex> args;
        private final boolean distinct;

        private Measure(Agg agg, List<Rex> args, boolean distinct) {
            this.agg = agg;
            this.args = args;
            this.distinct = distinct;
        }

        @NotNull
        public Agg getAgg() {
            return agg;
        }

        @NotNull
        public List<Rex> getArgs() {
            return args;
        }

        public boolean isDistinct() {
            return distinct;
        }

        @NotNull
        public Measure copy(@NotNull List<Rex> args) {
            return new Measure(agg, args, distinct);
        }
    }

    private static class Impl extends RelAggregate {

        private final Rel input;
        private final List<Measure> measures;
        private final List<Rex> groups;

        private Impl(Rel input, List<Measure> measures, List<Rex> groups) {
            this.input = input;
            this.measures = measures;
            this.groups = groups;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Measure> getMeasures() {
            return measures;
        }

        @NotNull
        @Override
        public List<Rex> getGroups() {
            return groups;
        }

        @NotNull
        @Override
        public RelAggregate copy(@NotNull Rel input) {
            return new Impl(input, measures, groups);
        }

        /**
         * @return copy with new input and args (non-final).
         */
        @NotNull
        @Override
        public RelAggregate copy(@NotNull Rel input, @NotNull List<Measure> measures, @NotNull List<Rex> groups) {
            return new Impl(input, measures, groups);
        }
    }
}
