package org.partiql.plan.builder

import org.partiql.plan.Collation
import org.partiql.plan.Exclusion
import org.partiql.plan.JoinType
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate

/**
 * DataFrame style fluent-builder for PartiQL logical plans.
 *
 * TODO schemas and field names.
 */
@Suppress("LocalVariableName")
public class RelBuilder private constructor(builder: Builder) {

    // DO NOT USE FINAL MEMBERS
    private var self: Builder = builder

    /**
     * Invoke the builder with the default [PlanFactory] implementation.
     */
    public fun build(): Rel = build(PlanFactory.STANDARD)

    /**
     * Invoke the builder with the given [PlanFactory] implementation.
     */
    public fun build(factory: PlanFactory): Rel = self.build(factory)

    /**
     * This object holds named constructors for the [RelBuilder] class.
     */
    public companion object {

        /**
         * Initialize a logical scan operator builder.
         */
        @JvmStatic
        public fun scan(input: RexBuilder): RelBuilder = RelBuilder {
            val _input = input.build(it)
            it.relScan(_input)
        }

        /**
         * Initialize a logical iterate operator builder.
         */
        @JvmStatic
        public fun iterate(input: RexBuilder): RelBuilder = RelBuilder {
            val _input = input.build(it)
            it.relIterate(_input)
        }

        /**
         * Initialize a logical unpivot operator builder.
         */
        @JvmStatic
        public fun unpivot(input: RexBuilder): RelBuilder = RelBuilder {
            val _input = input.build(it)
            it.relUnpivot(_input)
        }
    }

    /**
     * Appends a RelAggregate to the current operator builder.
     */
    public fun aggregate(
        measures: List<RelAggregate.Measure>,
        groups: List<RexBuilder>,
    ): RelBuilder = RelBuilder {
        val _input = self.build(it)
        val _measures = measures
        val _groups = groups.map { group -> group.build(it) }
        it.relAggregate(_input, _measures, _groups)
    }

    public fun distinct(): RelBuilder = RelBuilder {
        val _input = self.build(it)
        it.relDistinct(_input)
    }

    /**
     * Appends a RelExcept to the current operator builder.
     */
    public fun except(rhs: Rel): RelBuilder = RelBuilder {
        val lhs = self.build(it)
        it.relIntersect(lhs, rhs)
    }

    /**
     * Appends a RelExclude to the current operator builder.
     */
    public fun exclude(exclusions: List<Exclusion>): RelBuilder = RelBuilder {
        val _input = self.build(it)
        it.relExclude(_input, exclusions)
    }

    /**
     * Appends a RelFilter to the current operator builder.
     */
    public fun filter(predicate: RexBuilder): RelBuilder = RelBuilder {
        val _input = self.build(it)
        val _predicate = predicate.build(it)
        it.relFilter(_input, _predicate)
    }

    /**
     * Appends a RelIntersect to the current operator builder.
     */
    public fun intersect(rhs: RelBuilder): RelBuilder = RelBuilder {
        val _lhs = self.build(it)
        val _rhs = rhs.build(it)
        it.relIntersect(_lhs, _rhs)
    }

    /**
     * Appends a RelJoin to the current operator builder for LATERAL CROSS JOIN.
     */
    public fun join(rhs: RelBuilder): RelBuilder = join(rhs, null, JoinType.INNER())

    /**
     * Appends a RelJoin to the current operator builder for INNER JOIN ON <condition>.
     *
     * @param rhs
     * @param condition
     * @return
     */
    public fun join(
        rhs: RelBuilder,
        condition: RexBuilder,
    ): RelBuilder = join(rhs, condition, JoinType.INNER())

    /**
     * Appends a RelJoin to the current operator builder for [LEFT|RIGHT|INNER|FULL] JOIN.
     *
     * @param rhs
     * @param type
     * @return
     */
    public fun join(rhs: RelBuilder, type: JoinType): RelBuilder = join(rhs, null, type)

    /**
     * Appends a RelJoin to the current operator builder for [LEFT|RIGHT|INNER|FULL] JOIN ON <condition>.
     *
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun join(
        rhs: RelBuilder,
        condition: RexBuilder?,
        type: JoinType,
    ): RelBuilder = RelBuilder {
        val _lhs = self.build(it)
        val _rhs = rhs.build(it)
        val _condition = condition?.build(it)
        it.relJoin(_lhs, _rhs, _condition, type)
    }

    /**
     * Appends a RelLimit to the current operator builder.
     *
     * @param limit
     * @return
     */
    public fun limit(limit: RexBuilder): RelBuilder = RelBuilder {
        val _input = self.build(it)
        val _limit = limit.build(it)
        it.relLimit(_input, _limit)
    }

    /**
     * Appends a RelOffset to the current operator builder.
     *
     * @param offset
     * @return
     */
    public fun offset(offset: RexBuilder): RelBuilder = RelBuilder {
        val _input = self.build(it)
        val _offset = offset.build(it)
        it.relOffset(_input, _offset)
    }

    /**
     * Appends a RelProject to the current operator builder.
     *
     * @param projections
     * @return
     */
    public fun project(vararg projections: RexBuilder): RelBuilder = project(projections.toList())

    /**
     * Appends a RelProject to the current operator builder.
     *
     * @param projections
     * @return
     */
    public fun project(projections: List<RexBuilder>): RelBuilder = RelBuilder {
        val _input = self.build(it)
        val _projections = projections.map { rex -> rex.build(it) }
        it.relProject(_input, _projections)
    }

    /**
     * Appends a RelSort to the current operator builder.
     *
     * @param collations
     * @return
     */
    public fun sort(collations: List<Collation>): RelBuilder = RelBuilder {
        val _input = self.build(it)
        it.relSort(_input, collations)
    }

    /**
     * Appends a RelUnion to the current operator builder.
     */
    public fun union(rhs: Rel): RelBuilder = RelBuilder {
        val lhs = self.build(it)
        it.relUnion(lhs, rhs)
    }

    /**
     * Appends a RexPivot to the current relational operator – this is a rel->rex projection.
     */
    public fun pivot(
        key: RexBuilder,
        value: RexBuilder,
    ): RexBuilder = RexBuilder.pivot(this, key, value)

    /**
     * Appends a RexSelect to the current relation operator – this is a rel->rex projection.
     */
    public fun select(constructor: RexBuilder): RexBuilder = RexBuilder.select(this, constructor)

    // PRIVATE FUNCTIONAL INTERFACE COMPILES DOWN TO PRIVATE STATIC METHODS.
    private fun interface Builder {
        fun build(factory: PlanFactory): Rel
    }
}
