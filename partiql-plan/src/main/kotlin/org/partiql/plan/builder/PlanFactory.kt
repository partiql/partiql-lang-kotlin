package org.partiql.plan.builder

import org.partiql.eval.value.Datum
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
import org.partiql.plan.operator.rex.RexCall
import org.partiql.plan.operator.rex.RexCallImpl
import org.partiql.plan.operator.rex.RexCase
import org.partiql.plan.operator.rex.RexCaseImpl
import org.partiql.plan.operator.rex.RexCast
import org.partiql.plan.operator.rex.RexCastImpl
import org.partiql.plan.operator.rex.RexCoalesce
import org.partiql.plan.operator.rex.RexCoalesceImpl
import org.partiql.plan.operator.rex.RexCollection
import org.partiql.plan.operator.rex.RexCollectionImpl
import org.partiql.plan.operator.rex.RexLit
import org.partiql.plan.operator.rex.RexLitImpl
import org.partiql.plan.operator.rex.RexPivot
import org.partiql.plan.operator.rex.RexPivotImpl
import org.partiql.plan.operator.rex.RexSelect
import org.partiql.plan.operator.rex.RexSelectImpl
import org.partiql.plan.operator.rex.RexSpread
import org.partiql.plan.operator.rex.RexSpreadImpl
import org.partiql.plan.operator.rex.RexStruct
import org.partiql.plan.operator.rex.RexStructImpl
import org.partiql.plan.operator.rex.RexSubquery
import org.partiql.plan.operator.rex.RexSubqueryComp
import org.partiql.plan.operator.rex.RexSubqueryCompImpl
import org.partiql.plan.operator.rex.RexSubqueryImpl
import org.partiql.plan.operator.rex.RexSubqueryIn
import org.partiql.plan.operator.rex.RexSubqueryInImpl
import org.partiql.plan.operator.rex.RexSubqueryTest
import org.partiql.plan.operator.rex.RexSubqueryTestImpl
import org.partiql.plan.operator.rex.RexTable
import org.partiql.plan.operator.rex.RexTableImpl
import org.partiql.plan.operator.rex.RexVar
import org.partiql.plan.operator.rex.RexVarImpl
import org.partiql.types.PType

/**
 * The [PlanFactory] factory is used by builders and readers to provide concrete implementations of plan interfaces.
 *
 * This is an interface with default implementations; we cannot use default values or @JvmOverloads. There are a handful
 * of overloads where default values are inserted (such as set operator quantifiers) or where omission of certain fields
 * changes the logical operator
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

    // --- REL OPERATORS ------------------------------------------------------------------------------------------------

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
     * Create a [RelExcept] instance for the default EXCEPT.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relExcept(lhs: Rel, rhs: Rel): RelExcept = relExcept(lhs, rhs, false)

    /**
     * Create a [RelExcept] instance for EXCEPT [ALL|DISTINCT].
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
     * Create a [RelIntersect] instance for the default INTERSECT.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relIntersect(lhs: Rel, rhs: Rel): RelIntersect = relIntersect(lhs, rhs, false)

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
    public fun relJoin(lhs: Rel, rhs: Rel): RelJoin = relJoin(lhs, rhs, condition = null, RelJoinType.INNER)

    /**
     * Create a [RelJoin] instance.
     *
     * @param lhs
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun relJoin(lhs: Rel, rhs: Rel, condition: Rex?, type: RelJoinType): RelJoin =
        RelJoinImpl(lhs, rhs, condition, type)

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
     * Create a [RelUnion] instance for the default UNION.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relUnion(lhs: Rel, rhs: Rel): RelUnion = relUnion(lhs, rhs, false)

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

    // --- REX OPERATORS ------------------------------------------------------------------------------------------------

    /**
     * Create a [RexCall] instance.
     *
     * @param function
     * @param args
     * @return
     */
    public fun rexCall(function: String, args: List<Rex>): RexCall = RexCallImpl(function, args)

    /**
     * Create a [RexCase] instance for a searched case-when.
     *
     * @param branches
     * @param default
     * @return
     */
    public fun rexCase(branches: List<RexCase.Branch>, default: Rex?): RexCase = rexCase(null, branches, default)

    /**
     * Create a [RexCase] instance for a case-when.
     *
     * @param match
     * @param branches
     * @param default
     * @return
     */
    public fun rexCase(match: Rex?, branches: List<RexCase.Branch>, default: Rex?): RexCase =
        RexCaseImpl(match, branches, default)

    /**
     * Create a [RexCast] instance.
     *
     * @param operand
     * @param target
     * @return
     */
    public fun rexCast(operand: Rex, target: PType): RexCast = RexCastImpl(operand, target)

    /**
     * Create a [RexCoalesce] instance.
     *
     * @param args
     * @return
     */
    public fun rexCoalesce(args: List<Rex>): RexCoalesce = RexCoalesceImpl(args)

    /**
     * Create a [RexVar] instance.
     *
     * @param depth
     * @param offset
     * @return
     */
    public fun rexCol(depth: Int, offset: Int): RexVar = RexVarImpl(depth, offset)

    /**
     * Create a [RexCollection] instance.
     *
     * @param values
     * @return
     */
    public fun rexCollection(values: List<Rex>): RexCollection = RexCollectionImpl(values)

    /**
     * Create a [RexLit] instance.
     *
     * @param value
     * @return
     */
    public fun rexLit(value: Datum): RexLit = RexLitImpl(value)

    // TODO PATHS

    /**
     * Create a [RexPivot] instance.
     *
     * @param input
     * @param key
     * @param value
     * @return
     */
    public fun rexPivot(input: Rel, key: Rex, value: Rex): RexPivot = RexPivotImpl(input, key, value)

    /**
     * Create a [RexSelect] instance.
     *
     * @param input
     * @param constructor
     * @return
     */
    public fun rexSelect(input: Rel, constructor: Rex): RexSelect = RexSelectImpl(input, constructor)

    /**
     * Create a [RexSpread] instance.
     *
     * @param args
     * @return
     */
    public fun rexSpread(args: List<Rex>): RexSpread = RexSpreadImpl(args)

    /**
     * Create a [RexStruct] instance.
     *
     * @param fields
     * @return
     */
    public fun rexStruct(fields: List<RexStruct.Field>): RexStruct = RexStructImpl(fields)

    /**
     * Create a [RexSubquery] instance.
     *
     * @param rel
     * @return
     */
    public fun rexSubquery(rel: Rel): RexSubquery = RexSubqueryImpl(rel)

    /**
     * Create a [RexSubqueryComp] instance.
     *
     * @param args
     * @param comp
     * @param rel
     * @return
     */
    public fun rexSubqueryComp(args: List<Rex>, comp: RexSubqueryComp.Comp, rel: Rel): RexSubqueryComp =
        RexSubqueryCompImpl(args, comp, null, rel)

    /**
     * Create a [RexSubqueryComp] instance.
     *
     * @param args
     * @param comp
     * @param quantifier
     * @param rel
     * @return
     */
    public fun rexSubqueryComp(
        args: List<Rex>,
        comp: RexSubqueryComp.Comp,
        quantifier: RexSubqueryComp.Quantifier?,
        rel: Rel,
    ): RexSubqueryComp = RexSubqueryCompImpl(args, comp, quantifier, rel)

    /**
     * Create a [RexSubqueryIn] instance for single argument.
     *
     * @param arg
     * @param rel
     * @return
     */
    public fun rexSubqueryIn(arg: Rex, rel: Rel): RexSubqueryIn = rexSubqueryIn(listOf(arg), rel)

    /**
     * Create a [RexSubqueryIn] instance for a list argument.
     *
     * @param args
     * @param rel
     * @return
     */
    public fun rexSubqueryIn(args: List<Rex>, rel: Rel): RexSubqueryIn = RexSubqueryInImpl(args, rel)

    /**
     * Create a [RexSubqueryTest] instance.
     *
     * @param test
     * @param rel
     * @return
     */
    public fun rexSubqueryTest(test: RexSubqueryTest.Test, rel: Rel): RexSubqueryTest = RexSubqueryTestImpl(test, rel)

    /**
     * Create a [RexTable] instance.
     *
     * @param catalog
     * @param name
     * @return
     */
    public fun rexTable(catalog: String, name: String): RexTable = RexTableImpl(catalog, name)
}
