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
     * Creates a new {@link RelAggregate} instance with reference-based measures for lazy resolution.
     * @param input the input
     * @param measureRefs the measure references
     * @param groups the groups
     * @return new {@link RelAggregate} instance
     */
    @NotNull
    public static RelAggregate createRef(@NotNull Rel input, @NotNull List<MeasureRef> measureRefs, @NotNull List<Rex> groups) {
        return new ImplRef(input, measureRefs, groups);
    }

    /**
     * Creates a new {@link Measure} instance.
     * @param agg the aggregation function
     * @param args the arguments
     * @param distinct the distinct flag
     * @return new {@link Measure} instance
     * @deprecated Use {@link #measureRef(int, int, List, boolean)} with {@code PartiQLPlanner.builder().useRefs()}.
     */
    @Deprecated
    @NotNull
    public static Measure measure(@NotNull Agg agg, @NotNull List<Rex> args, boolean distinct) {
        return new Measure(agg, args, distinct);
    }

    /**
     * Creates a new {@link MeasureRef} instance with integer references for lazy resolution.
     * @param catalogId the catalog identifier assigned during planning
     * @param aggId the aggregate identifier within the catalog
     * @param args the arguments
     * @param distinct the distinct flag
     * @return new {@link MeasureRef} instance
     */
    @NotNull
    public static MeasureRef measureRef(int catalogId, int aggId, @NotNull List<Rex> args, boolean distinct) {
        return new MeasureRef(catalogId, aggId, args, distinct);
    }

    /**
     * Gets the input.
     * @return the input (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Gets the measures. Returns an empty list if this aggregate uses measure references instead.
     * @return the measures (arg)
     */
    @NotNull
    public abstract List<Measure> getMeasures();

    /**
     * Gets the measure references for lazy resolution. Returns an empty list if this aggregate uses embedded measures.
     * @return the measure references
     */
    @NotNull
    public List<MeasureRef> getMeasureRefs() {
        return List.of();
    }

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
     *
     * @deprecated Use {@link MeasureRef} with {@code PartiQLPlanner.builder().useRefs()} for thread-safe plans.
     */
    @Deprecated
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

    /**
     * An aggregation function reference for lazy resolution at execution time.
     */
    public static class MeasureRef {

        private final int catalogId;
        private final int aggId;
        private final List<Rex> args;
        private final boolean distinct;

        private MeasureRef(int catalogId, int aggId, List<Rex> args, boolean distinct) {
            this.catalogId = catalogId;
            this.aggId = aggId;
            this.args = args;
            this.distinct = distinct;
        }

        public int getCatalogId() {
            return catalogId;
        }

        public int getAggId() {
            return aggId;
        }

        @NotNull
        public List<Rex> getArgs() {
            return args;
        }

        public boolean isDistinct() {
            return distinct;
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

    private static class ImplRef extends RelAggregate {

        private final Rel input;
        private final List<MeasureRef> measureRefs;
        private final List<Rex> groups;

        private ImplRef(Rel input, List<MeasureRef> measureRefs, List<Rex> groups) {
            this.input = input;
            this.measureRefs = measureRefs;
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
            return List.of();
        }

        @NotNull
        @Override
        public List<MeasureRef> getMeasureRefs() {
            return measureRefs;
        }

        @NotNull
        @Override
        public List<Rex> getGroups() {
            return groups;
        }

        @NotNull
        @Override
        public RelAggregate copy(@NotNull Rel input) {
            return new ImplRef(input, measureRefs, groups);
        }

        @NotNull
        @Override
        public RelAggregate copy(@NotNull Rel input, @NotNull List<Measure> measures, @NotNull List<Rex> groups) {
            return new ImplRef(input, measureRefs, groups);
        }
    }
}
