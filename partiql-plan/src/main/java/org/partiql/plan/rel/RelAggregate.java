package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Aggregation;

import java.util.Collection;
import java.util.List;

/**
 * The logical aggregation abstract base class.
 */
public abstract class RelAggregate extends RelBase {

    // TODO GROUP STRATEGY: https://github.com/partiql/partiql-lang-kotlin/issues/1664

    private final RelType type = null;
    private List<Operator> children = null;

    /**
     * @return the input (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    @NotNull
    public abstract Collection<Measure> getMeasures();

    @NotNull
    public abstract Collection<Rex> getGroups();

    @NotNull
    @Override
    public final RelType getType() {
        if (type == null) {
            throw new UnsupportedOperationException("Derive type is not implemented");
        }
        return type;
    }

    @NotNull
    @Override
    public final List<Operator> getChildren() {
        if (children == null) {
            Rel c0 = getInput();
            children = List.of(c0);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitAggregate(this, ctx);
    }

    /**
     * An aggregation function along with its arguments and any additional filters (e.g. DISTINCT).
     */
    public static class Measure {

        private final Aggregation agg;
        private final Collection<Rex> args;
        private final Boolean distinct;

        public Measure(Aggregation agg, Collection<Rex> args, Boolean distinct) {
            this.agg = agg;
            this.args = args;
            this.distinct = distinct;
        }

        @NotNull
        public Aggregation getAgg() {
            return agg;
        }

        @NotNull
        public Collection<Rex> getArgs() {
            return args;
        }

        @NotNull
        public Boolean isDistinct() {
            return distinct;
        }
    }
}
