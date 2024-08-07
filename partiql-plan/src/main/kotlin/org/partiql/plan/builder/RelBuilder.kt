package org.partiql.plan.builder

import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelAggregateCall
import org.partiql.plan.operator.rel.RelCollation
import org.partiql.plan.operator.rel.RelExcludePath
import org.partiql.plan.operator.rel.RelJoinType
import org.partiql.plan.operator.rex.Rex

/**
 * DataFrame style fluent-builder for PartiQL logical plans.
 *
 * TODO schemas and field names.
 */
public class RelBuilder private constructor(rel: _Rel) {

    // KEEP FINAL TO ENSURE ITS NEVER MUTATED.
    private val _rel: _Rel = rel

    // PRIVATE FUNCTIONAL INTERFACE TO ENSURE WE GET A JVM LAMBDA.
    @Suppress("ClassName")
    private fun interface _Rel { fun build(factory: PlanFactory): Rel }

    /**
     * Invoke the builder with the given [PlanFactory] implementation.
     */
    public fun build(factory: PlanFactory): Rel = _rel.build(factory)

    /**
     * Invoke the builder with the default [PlanFactory] implementation.
     */
    public fun build(): Rel = build(PlanFactory.STANDARD)

    /**
     * Appends a RelAggregate to the current operator builder.
     */
    public fun aggregate(calls: List<RelAggregateCall>): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relAggregate(input, calls)
    }

    public fun distinct(): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relDistinct(input)
    }

    /**
     * Appends a RelExcept to the current operator builder.
     */
    public fun except(rhs: Rel): RelBuilder = RelBuilder { factory ->
        val lhs = _rel.build(factory)
        factory.relIntersect(lhs, rhs)
    }

    /**
     * Appends a RelExclude to the current operator builder.
     */
    public fun exclude(paths: List<RelExcludePath>): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relExclude(input, paths)
    }

    /**
     * Appends a RelFilter to the current operator builder.
     */
    public fun filter(predicate: Rex): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relFilter(input, predicate)
    }

    /**
     * Appends a RelIntersect to the current operator builder.
     */
    public fun intersect(rhs: Rel): RelBuilder = RelBuilder { factory ->
        val lhs = _rel.build(factory)
        factory.relIntersect(lhs, rhs)
    }

    /**
     * Appends a RelJoin to the current operator builder for LATERAL CROSS JOIN.
     */
    public fun join(rhs: Rel): RelBuilder = join(rhs, null, RelJoinType.INNER)

    /**
     * Appends a RelJoin to the current operator builder for INNER JOIN ON <condition>.
     *
     * @param rhs
     * @param condition
     * @return
     */
    public fun join(rhs: Rel, condition: Rex): RelBuilder = join(rhs, condition, RelJoinType.INNER)

    /**
     * Appends a RelJoin to the current operator builder for [LEFT|RIGHT|INNER|FULL] JOIN.
     *
     * @param rhs
     * @param type
     * @return
     */
    public fun join(rhs: Rel, type: RelJoinType): RelBuilder = join(rhs, null, type)

    /**
     * Appends a RelJoin to the current operator builder for [LEFT|RIGHT|INNER|FULL] JOIN ON <condition>.
     *
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun join(rhs: Rel, condition: Rex?, type: RelJoinType): RelBuilder = RelBuilder { factory ->
        val lhs = _rel.build(factory)
        factory.relJoin(lhs, rhs, condition, type)
    }

    /**
     * Appends a RelLimit to the current operator builder.
     *
     * @param limit
     * @return
     */
    public fun limit(limit: Rex): RelBuilder = RelBuilder { factory ->
        val rel = _rel.build(factory)
        factory.relLimit(rel, limit)
    }

    /**
     * Appends a RelOffset to the current operator builder.
     *
     * @param offset
     * @return
     */
    public fun offset(offset: Rex): RelBuilder = RelBuilder { factory ->
        val rel = _rel.build(factory)
        factory.relLimit(rel, offset)
    }

    /**
     * Appends a RelProject to the current operator builder.
     *
     * @param projections
     * @return
     */
    public fun project(projections: List<Rex>): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relProject(input, projections)
    }

    /**
     * Appends a RelSort to the current operator builder.
     *
     * @param collations
     * @return
     */
    public fun sort(collations: List<RelCollation>): RelBuilder = RelBuilder { factory ->
        val input = _rel.build(factory)
        factory.relSort(input, collations)
    }

    /**
     * Appends a RelUnion to the current operator builder.
     */
    public fun union(rhs: Rel): RelBuilder = RelBuilder { factory ->
        val lhs = _rel.build(factory)
        factory.relUnion(lhs, rhs)
    }

    // /**
    //  * The SELECT VALUE operator.
    //  *
    //  * @param constructor
    //  * @return
    //  */
    // public fun select(constructor: Rex): RexBuilder {
    //     val rex = object : RexSelect.Base(_rel, constructor) {}
    //     return RexBuilder(rex)
    // }
    //
    // /**
    //  * The PIVOT relation-expression projection.
    //  */
    // public fun pivot(key: Rex, value: Rex): RexBuilder {
    //     val rex = object : RexPivot.Base(_rel, key, value) {}
    //     return RexBuilder(rex)
    // }

    /**
     * PlanBuilder constructor remains private, and all logic for the static methods lives here.
     */
    public companion object {

        /**
         * Initialize a logical scan operator builder.
         */
        @JvmStatic
        public fun scan(input: Rex): RelBuilder = RelBuilder { it.relScan(input) }

        /**
         * Initialize a logical iterate operator builder.
         */
        @JvmStatic
        public fun iterate(input: Rex): RelBuilder  = RelBuilder { it.relIterate(input) }

        /**
         * Initialize a logical unpivot operator builder.
         *
         * @param input
         * @return
         */
        @JvmStatic
        public fun unpivot(input: Rex): RelBuilder = RelBuilder { it.relUnpivot(input) }
    }
}
