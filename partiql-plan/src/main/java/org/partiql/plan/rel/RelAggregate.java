package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Aggregation;

import java.util.List;

// TODO GROUP STRATEGY: https://github.com/partiql/partiql-lang-kotlin/issues/1664

/**
 * The logical aggregation abstract base class.
 */
public abstract class RelAggregate extends RelBase {

    /**
     * @return new {@link RelAggregate} instance
     */
    @NotNull
    public static RelAggregate create(@NotNull Rel input, @NotNull List<Measure> measures, @NotNull List<Rex> groups) {
        return new Impl(input, measures, groups);
    }

    /**
     * @return new  {@link Measure} instance
     */
    @NotNull
    public static Measure measure(@NotNull Aggregation agg, @NotNull List<Rex> args, @NotNull  Boolean distinct) {
        return new Measure(agg, args, distinct);
    }

    /**
     * @return the input (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return the measures (arg)
     */
    @NotNull
    public abstract List<Measure> getMeasures();

    /**
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
    protected final List<Operator> operands() {
        Rel c0 = getInput();
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
     * <br>
     * TODO unnest ??
     */
    public static class Measure {

        private final Aggregation agg;
        private final List<Rex> args;
        private final Boolean distinct;

        private Measure(Aggregation agg, List<Rex> args, Boolean distinct) {
            this.agg = agg;
            this.args = args;
            this.distinct = distinct;
        }

        @NotNull
        public Aggregation getAgg() {
            return agg;
        }

        @NotNull
        public List<Rex> getArgs() {
            return args;
        }

        @NotNull
        public Boolean isDistinct() {
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
