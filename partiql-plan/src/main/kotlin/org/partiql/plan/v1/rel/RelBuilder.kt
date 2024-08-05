package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex
import org.partiql.plan.v1.rex.RexBuilder
import org.partiql.plan.v1.rex.RexPivot
import org.partiql.plan.v1.rex.RexSelect

/**
 * DataFrame style fluent-builder for PartiQL logical plans.
 *
 * TODO schemas and field names.
 */
public class RelBuilder private constructor(rel: Rel) {

    // KEEP FINAL TO ENSURE ITS NEVER MUTATED
    private val _rel: Rel = rel

    /**
     * Returns the [Rel] created by the builder.
     */
    public fun build(): Rel = _rel

    public fun aggregate(calls: List<RelAggregateCall>): RelBuilder {
        val rel = object : RelAggregate.Base(_rel, calls) {}
        return RelBuilder(rel)
    }

    public fun distinct(): RelBuilder {
        val rel = object : RelDistinct.Base(_rel) {}
        return RelBuilder(rel)
    }

    public fun except(rhs: Rel): RelBuilder {
        val rel = object : RelExcept.Base(_rel, rhs) {}
        return RelBuilder(rel)
    }

    public fun filter(condition: Rex): RelBuilder {
        val rel = object : RelFilter.Base(_rel, condition) {}
        return RelBuilder(rel)
    }

    public fun intersect(rhs: Rel): RelBuilder {
        val rel = object : RelIntersect.Base(_rel, rhs) {}
        return RelBuilder(rel)
    }

    /**
     * TODO other join types.
     */
    public fun join(rhs: Rel): RelBuilder {
        // TEMP!
        val condition = Rex.lit(true)
        val type = RelJoinType.INNER
        // END TEMP!
        val rel = object : RelJoin.Base(_rel, rhs, condition, type) {}
        return RelBuilder(rel)
    }

    public fun limit(limit: Rex): RelBuilder {
        val rel = object : RelLimit.Base(_rel, limit) {}
        return RelBuilder(rel)
    }

    public fun offset(offset: Rex): RelBuilder {
        val rel = object : RelOffset.Base(_rel, offset) {}
        return RelBuilder(rel)
    }

    public fun project(projections: List<Rex>): RelBuilder {
        val rel = object : RelProject.Base(_rel, projections) {}
        return RelBuilder(rel)
    }

    public fun sort(collations: List<RelCollation>): RelBuilder {
        val rel = object : RelSort.Base(_rel, collations) {}
        return RelBuilder(rel)
    }

    public fun union(rhs: Rel): RelBuilder {
        val rel = object : RelUnion.Base(_rel, rhs) {}
        return RelBuilder(rel)
    }

    /**
     * The SELECT VALUE operator.
     *
     * @param constructor
     * @return
     */
    public fun select(constructor: Rex): RexBuilder {
        val rex = object : RexSelect.Base(_rel, constructor) {}
        return RexBuilder(rex)
    }

    /**
     * The PIVOT relation-expression projection.
     */
    public fun pivot(key: Rex, value: Rex): RexBuilder {
        val rex = object : RexPivot.Base(_rel, key, value) {}
        return RexBuilder(rex)
    }

    /**
     * PlanBuilder constructor remains private, and all logic for the static methods lives here.
     */
    public companion object {

        /**
         * Initialize a [RelScan] builder.
         */
        @JvmStatic
        public fun scan(rex: Rex): RelBuilder {
            val rel = object : RelScan.Base(rex) {}
            return RelBuilder(rel)
        }

        /**
         * Initialize a [RelScanIndexed] builder.
         */
        @JvmStatic
        public fun scanIndexed(rex: Rex): RelBuilder {
            val rel = object : RelScanIndexed.Base(rex) {}
            return RelBuilder(rel)
        }

        @JvmStatic
        public fun unpivot(rex: Rex): RelBuilder {
            val rel = object : RelUnpivot.Base(rex) {}
            return RelBuilder(rel)
        }
    }
}
