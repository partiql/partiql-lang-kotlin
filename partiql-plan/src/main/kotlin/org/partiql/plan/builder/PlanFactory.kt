package org.partiql.plan.builder

import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelAggregate
import org.partiql.plan.operator.rel.RelAggregateCall
import org.partiql.plan.operator.rel.RelCollation
import org.partiql.plan.operator.rel.RelDistinct
import org.partiql.plan.operator.rel.RelExcept
import org.partiql.plan.operator.rel.RelExclude
import org.partiql.plan.operator.rel.RelExcludePath
import org.partiql.plan.operator.rel.RelFilter
import org.partiql.plan.operator.rel.RelIntersect
import org.partiql.plan.operator.rel.RelIterate
import org.partiql.plan.operator.rel.RelJoin
import org.partiql.plan.operator.rel.RelJoinType
import org.partiql.plan.operator.rel.RelLimit
import org.partiql.plan.operator.rel.RelOffset
import org.partiql.plan.operator.rel.RelProject
import org.partiql.plan.operator.rel.RelScan
import org.partiql.plan.operator.rel.RelSort
import org.partiql.plan.operator.rel.RelUnion
import org.partiql.plan.operator.rel.RelUnpivot
import org.partiql.plan.operator.rel.impl.RelAggregateImpl
import org.partiql.plan.operator.rel.impl.RelDistinctImpl
import org.partiql.plan.operator.rel.impl.RelExceptImpl
import org.partiql.plan.operator.rel.impl.RelExcludeImpl
import org.partiql.plan.operator.rel.impl.RelFilterImpl
import org.partiql.plan.operator.rel.impl.RelIntersectImpl
import org.partiql.plan.operator.rel.impl.RelIterateImpl
import org.partiql.plan.operator.rel.impl.RelJoinImpl
import org.partiql.plan.operator.rel.impl.RelLimitImpl
import org.partiql.plan.operator.rel.impl.RelOffsetImpl
import org.partiql.plan.operator.rel.impl.RelProjectImpl
import org.partiql.plan.operator.rel.impl.RelScanImpl
import org.partiql.plan.operator.rel.impl.RelSortImpl
import org.partiql.plan.operator.rel.impl.RelUnionImpl
import org.partiql.plan.operator.rel.impl.RelUnpivotImpl
import org.partiql.plan.operator.rex.Rex

/**
 * TODO
 */
public interface PlanFactory {

    // ALL MEMBERS SHOULD BE @JvmStatic
    public companion object {

        /**
         * A singleton of the [PlanFactory] with all the default implementations.
         */
        @JvmStatic
        public val STANDARD: PlanFactory = object : PlanFactory {}
    }

    /**
     * Create a [RelAggregate] instance.
     *
     * @param input
     * @param calls [RelAggregateCall
     *
     * @param input
     */
    public fun relAggregate(input: Rel, calls: List<RelAggregateCall>): RelAggregate = RelAggregateImpl(input, calls)

    /**
     * Create a [RelDistinct] instance.
     *
     * @param input
     * @return
     */
    public fun relDistinct(input: Rel): RelDistinct = RelDistinctImpl(input)

    /**
     * Create a [RelExcept] instance for EXCEPT DISTINCT.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relExcept(lhs: Rel, rhs: Rel): RelExcept = RelExceptImpl(lhs, rhs, false)

    /**
     * Create a [RelExcept] instance for EXCEPT.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relExcept(lhs: Rel, rhs: Rel, isAll: Boolean): RelExcept = RelExceptImpl(lhs, rhs, isAll)

    /**
     * Create a [RelExclude] instance.
     *
     * @param input
     * @param paths
     * @return
     */
    public fun relExclude(input: Rel, paths: List<RelExcludePath>): RelExclude = RelExcludeImpl(input, paths)

    /**
     * Create a [RelFilter] instance.
     *
     * @param input
     * @param predicate
     * @return
     */
    public fun relFilter(input: Rel, predicate: Rex): RelFilter = RelFilterImpl(input, predicate)

    /**
     * Create a [RelIntersect] instance for INTERSECT.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relIntersect(lhs: Rel, rhs: Rel): RelIntersect = RelIntersectImpl(lhs, rhs, false)

    /**
     * Create a [RelIntersect] instance for INTERSECT [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relIntersect(lhs: Rel, rhs: Rel, isAll: Boolean): RelIntersect = RelIntersectImpl(lhs, rhs, isAll)

    /**
     * Create a [RelIterate] instance.
     *
     * @param input
     * @return
     */
    public fun relIterate(input: Rex): RelIterate = RelIterateImpl(input)

    /**
     * Create a [RelJoin] instance for a lateral cross join.
     *
     *   - <lhs>, <rhs>
     *   - <lhs> CROSS JOIN <rhs>
     *   - <lhs> JOIN <rhs> ON TRUE
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relJoin(lhs: Rel, rhs: Rel): RelJoin = RelJoinImpl(lhs, rhs, condition = null, RelJoinType.INNER)

    /**
     * Create a [RelJoin] instance.
     *
     * @param lhs
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun relJoin(lhs: Rel, rhs: Rel, condition: Rex?, type: RelJoinType): RelJoin = RelJoinImpl(lhs, rhs, condition, type)

    /**
     * Create a [RelLimit] instance.
     *
     * @param input
     * @param limit
     * @return
     */
    public fun relLimit(input: Rel, limit: Rex): RelLimit = RelLimitImpl(input, limit)

    /**
     * Create a [RelLimit] instance.
     *
     * @param input
     * @param offset
     * @return
     */
    public fun relOffset(input: Rel, offset: Rex): RelOffset = RelOffsetImpl(input, offset)

    /**
     * Create a [RelProject] instance.
     *
     * @param input
     * @param projections
     * @return
     */
    public fun relProject(input: Rel, projections: List<Rex>): RelProject = RelProjectImpl(input, projections)

    /**
     * Create a [RelScan] instance.
     *
     * @param input
     * @return
     */
    public fun relScan(input: Rex): RelScan = RelScanImpl(input)

    /**
     * Create a [RelSort] instance.
     *
     * @param input
     * @param collations
     * @return
     */
    public fun relSort(input: Rel, collations: List<RelCollation>): RelSort = RelSortImpl(input, collations)

    /**
     * Create a [RelUnion] instance for UNION.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relUnion(lhs: Rel, rhs: Rel): RelUnion = RelUnionImpl(lhs, rhs, false)

    /**
     * Create a [RelUnion] instance for UNION [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relUnion(lhs: Rel, rhs: Rel, isAll: Boolean): RelUnion = RelUnionImpl(lhs, rhs, isAll)

    /**
     * Create a [RelUnpivot] instance.
     *
     * @param input
     * @return
     */
    public fun relUnpivot(input: Rex): RelUnpivot = RelUnpivotImpl(input)
}
