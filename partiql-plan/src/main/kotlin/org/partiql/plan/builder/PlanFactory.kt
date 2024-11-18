package org.partiql.plan.builder

import org.partiql.plan.AggregateCall
import org.partiql.plan.Collation
import org.partiql.plan.Exclusion
import org.partiql.plan.JoinType
import org.partiql.plan.RelAggregateCallImpl
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelAggregateImpl
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelCorrelateImpl
import org.partiql.plan.rel.RelDistinct
import org.partiql.plan.rel.RelDistinctImpl
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelExceptImpl
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelExcludeImpl
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelFilterImpl
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelIntersectImpl
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelIterateImpl
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelJoinImpl
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelLimitImpl
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelOffsetImpl
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelProjectImpl
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelScanImpl
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelSortImpl
import org.partiql.plan.rel.RelType
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rel.RelUnionImpl
import org.partiql.plan.rel.RelUnpivot
import org.partiql.plan.rel.RelUnpivotImpl
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexArrayImpl
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexBagImpl
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCallDynamic
import org.partiql.plan.rex.RexCallDynamicImpl
import org.partiql.plan.rex.RexCallImpl
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCaseImpl
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCastImpl
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexCoalesceImpl
import org.partiql.plan.rex.RexError
import org.partiql.plan.rex.RexErrorImpl
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexLitImpl
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexNullIfImpl
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathIndexImpl
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathKeyImpl
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexPathSymbolImpl
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexPivotImpl
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexSelectImpl
import org.partiql.plan.rex.RexSpread
import org.partiql.plan.rex.RexSpreadImpl
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexStructImpl
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexSubqueryComp
import org.partiql.plan.rex.RexSubqueryCompImpl
import org.partiql.plan.rex.RexSubqueryImpl
import org.partiql.plan.rex.RexSubqueryIn
import org.partiql.plan.rex.RexSubqueryInImpl
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.plan.rex.RexSubqueryTestImpl
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexTableImpl
import org.partiql.plan.rex.RexType
import org.partiql.plan.rex.RexVar
import org.partiql.plan.rex.RexVarImpl
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum
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
    public fun relAggregate(
        input: Rel,
        calls: List<AggregateCall>,
        groups: List<Rex>,
    ): RelAggregate = RelAggregateImpl(input, calls, groups)

    /**
     * Create a [AggregateCall] instance.
     *
     * @param aggregation
     * @param args
     * @param isDistinct
     * @return
     */
    public fun relAggregateCall(
        aggregation: Aggregation,
        args: List<Rex>,
        isDistinct: Boolean = false,
    ): AggregateCall = RelAggregateCallImpl(aggregation, args, isDistinct)

    /**
     * Create a [RelCorrelate] instance for a lateral cross join.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relCorrelate(lhs: Rel, rhs: Rel): RelCorrelate = relCorrelate(lhs, rhs, JoinType.INNER)

    /**
     * Create a [RelCorrelate] instance for a lateral join.
     *
     * @param lhs
     * @param rhs
     * @param joinType
     * @return
     */
    public fun relCorrelate(lhs: Rel, rhs: Rel, joinType: JoinType): RelCorrelate = RelCorrelateImpl(lhs, rhs, joinType)

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
    public fun relExcept(
        lhs: Rel,
        rhs: Rel,
        isAll: Boolean,
    ): RelExcept = RelExceptImpl(lhs, rhs, isAll)

    /**
     * Create a [RelExclude] instance.
     *
     * @param input
     * @param exclusions
     * @return
     */
    public fun relExclude(input: Rel, exclusions: List<Exclusion>): RelExclude = RelExcludeImpl(input, exclusions)

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
    public fun relIntersect(
        lhs: Rel,
        rhs: Rel,
    ): RelIntersect = relIntersect(lhs, rhs, false)

    /**
     * Create a [RelIntersect] instance for INTERSECT [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relIntersect(
        lhs: Rel,
        rhs: Rel,
        isAll: Boolean,
    ): RelIntersect = RelIntersectImpl(lhs, rhs, isAll)

    /**
     * Create a [RelIterate] instance.
     *
     * @param input
     * @return
     */
    public fun relIterate(input: Rex): RelIterate = RelIterateImpl(input)

    /**
     * Create a [RelJoin] instance for a cross join.
     *
     *   - <lhs>, <rhs>
     *   - <lhs> CROSS JOIN <rhs>
     *   - <lhs> JOIN <rhs> ON TRUE
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun relJoin(lhs: Rel, rhs: Rel): RelJoin = relJoin(lhs, rhs, condition = null, JoinType.INNER)

    /**
     * Create a [RelJoin] instance.
     *
     * @param lhs
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun relJoin(
        lhs: Rel,
        rhs: Rel,
        condition: Rex?,
        type: JoinType,
        lhsSchema: RelType? = null,
        rhsSchema: RelType? = null,
    ): RelJoin = RelJoinImpl(lhs, rhs, condition, type, lhsSchema, rhsSchema)

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
    public fun relSort(input: Rel, collations: List<Collation>): RelSort = RelSortImpl(input, collations)

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
     * Create a [RexArray] instance with dynamic array type.
     *
     * @param values
     * @return
     */
    public fun rexArray(values: Collection<Rex>): RexArray = RexArrayImpl(values, RexType(PType.array()))

    /**
     * Create a [RexArray] instance with the given array type.
     *
     * @param values
     * @param type
     * @return
     */
    public fun rexArray(values: Collection<Rex>, type: RexType): RexArray = RexArrayImpl(values, type)

    /**
     * Create a [RexBag] instance with dynamic bag type.
     *
     * @param values
     * @return
     */
    public fun rexBag(values: Collection<Rex>): RexBag = RexBagImpl(values, RexType(PType.bag()))

    /**
     * Create a [RexBag] instance with the given array type.
     *
     * @param values
     * @param type
     * @return
     */
    public fun rexBag(values: Collection<Rex>, type: RexType): RexBag = RexBagImpl(values, type)

    /**
     * Create a [RexCall] instance.
     *
     * @param function
     * @param args
     * @return
     */
    public fun rexCall(function: Function.Instance, args: List<Rex>): RexCall = RexCallImpl(function, args)

    /**
     * Create a [RexCallDynamic] instance.
     *
     * @param name TODO
     * @param functions TODO
     * @param args TODO
     * @return TODO
     */
    public fun rexCallDynamic(name: String, functions: List<Function>, args: List<Rex>): RexCallDynamic =
        RexCallDynamicImpl(name, functions, args)

    /**
     * Create a [RexCallDynamic] instance.
     *
     * @param name TODO
     * @param functions TODO
     * @param args TODO
     * @param type specifies the output type of the dynamic dispatch. This may be specified if all candidate functions
     * return the same type.
     * @return TODO
     */
    public fun rexCallDynamic(name: String, functions: List<Function>, args: List<Rex>, type: PType): RexCallDynamic =
        RexCallDynamicImpl(name, functions, args, type)

    /**
     * Create a [RexCase] instance for a searched case-when with dynamic type.
     *
     * @param branches
     * @param default
     * @return
     */
    public fun rexCase(branches: List<RexCase.Branch>, default: Rex?): RexCase = rexCase(null, branches, default)

    /**
     * Create a [RexCase] instance for a searched case-when with the given type.
     *
     * @param branches
     * @param default
     * @param type
     * @return
     */
    public fun rexCase(branches: List<RexCase.Branch>, default: Rex?, type: RexType): RexCase =
        rexCase(null, branches, default, type)

    /**
     * Create a [RexCase] instance for a case-when with dynamic type.
     *
     * @param match
     * @param branches
     * @param default
     * @return
     */
    public fun rexCase(match: Rex?, branches: List<RexCase.Branch>, default: Rex?): RexCase =
        RexCaseImpl(match, branches, default, RexType.dynamic())

    /**
     * Create a [RexCase] instance for a case-when with the given type.
     *
     * @param match
     * @param branches
     * @param default
     * @param type
     * @return
     */
    public fun rexCase(match: Rex?, branches: List<RexCase.Branch>, default: Rex?, type: RexType): RexCase =
        RexCaseImpl(match, branches, default, type)

    /**
     * Create a [RexCast] instance.
     *
     * @param operand
     * @param target
     * @return
     */
    public fun rexCast(operand: Rex, target: PType): RexCast = RexCastImpl(operand, target)

    /**
     * Create a [RexCoalesce] instance with dynamic type.
     *
     * @param args
     * @return
     */
    public fun rexCoalesce(args: List<Rex>): RexCoalesce = RexCoalesceImpl(args, RexType.dynamic())

    /**
     * Create a [RexCoalesce] instance with the given type.
     *
     * @param args
     * @return
     */
    public fun rexCoalesce(args: List<Rex>, type: RexType): RexCoalesce = RexCoalesceImpl(args, type)

    /**
     * Create a [RexVar] instance with dynamic type.
     *
     * @param depth
     * @param offset
     * @return
     */
    public fun rexVar(depth: Int, offset: Int): RexVar = RexVarImpl(depth, offset, RexType.dynamic())

    /**
     * Create a [RexVar] instance with the given type.
     *
     * @param depth
     * @param offset
     * @param type
     * @return
     */
    public fun rexVar(depth: Int, offset: Int, type: RexType): RexVar = RexVarImpl(depth, offset, type)

    /**
     * TODO AUDIT ME
     * Create a [RexError] instance.
     */
    public fun rexError(type: PType): RexError = RexErrorImpl()

    /**
     * Create a [RexLit] instance.
     *
     * @param value
     * @return
     */
    public fun rexLit(value: Datum): RexLit = RexLitImpl(value)

    /**
     * Create a [RexNullIf] instance.
     */
    public fun rexNullIf(value: Rex, nullifier: Rex): RexNullIf = RexNullIfImpl(value, nullifier)

    /**
     * Create a [RexPathIndex] instance with dynamic type.
     *
     * @param operand
     * @param index
     * @return
     */
    public fun rexPathIndex(operand: Rex, index: Rex): RexPathIndex =
        RexPathIndexImpl(operand, index, RexType.dynamic())

    /**
     * Create a [RexPathIndex] instance with the given type.
     *
     * @param operand
     * @param index
     * @param type
     * @return
     */
    public fun rexPathIndex(operand: Rex, index: Rex, type: RexType): RexPathIndex =
        RexPathIndexImpl(operand, index, type)

    /**
     * Create a [RexPathKey] instance with dynamic type.
     *
     * @param operand
     * @param key
     * @return
     */
    public fun rexPathKey(operand: Rex, key: Rex): RexPathKey = RexPathKeyImpl(operand, key, RexType.dynamic())

    /**
     * Create a [RexPathKey] instance with the given type.
     *
     * @param operand
     * @param key
     * @param type
     * @return
     */
    public fun rexPathKey(operand: Rex, key: Rex, type: RexType): RexPathKey = RexPathKeyImpl(operand, key, type)

    /**
     * Create a [RexPathSymbol] instance with dynamic type.
     *
     * @param operand
     * @param symbol
     * @return
     */
    public fun rexPathSymbol(operand: Rex, symbol: String): RexPathSymbol =
        RexPathSymbolImpl(operand, symbol, RexType.dynamic())

    /**
     * Create a [RexPathSymbol] instance with the given type.
     *
     * @param operand
     * @param symbol
     * @param type
     * @return
     */
    public fun rexPathSymbol(operand: Rex, symbol: String, type: RexType): RexPathSymbol =
        RexPathSymbolImpl(operand, symbol, type)

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
     * Create a [RexSpread] instance with open struct type.
     *
     * @param args
     * @return
     */
    public fun rexSpread(args: List<Rex>): RexSpread = RexSpreadImpl(args, RexType(PType.struct()))

    /**
     * Create a [RexSpread] instance with the given type.
     *
     * @param args
     * @return
     */
    public fun rexSpread(args: List<Rex>, type: RexType): RexSpread = RexSpreadImpl(args, type)

    /**
     * Create a [RexStruct] instance with open struct type.
     *
     * @param fields
     * @return
     */
    public fun rexStruct(fields: List<RexStruct.Field>): RexStruct = RexStructImpl(fields, RexType(PType.struct()))

    /**
     * Create a [RexStruct] instance with the given type.
     *
     * @param fields
     * @return
     */
    public fun rexStruct(fields: List<RexStruct.Field>, type: RexType): RexStruct = RexStructImpl(fields, type)

    /**
     * Create a [RexSubquery] instance.
     *
     * TODO REMOVE constructor AND asScalar – TEMPORARY UNTIL SUBQUERIES ARE FIXED IN THE PLANNER.
     *
     * @param rel
     * @return
     */
    public fun rexSubquery(rel: Rel, constructor: Rex, asScalar: Boolean): RexSubquery =
        RexSubqueryImpl(rel, constructor, asScalar)

    /**
     * Create a [RexSubqueryComp] instance.
     *
     * @param args
     * @param comp
     * @param rel
     * @return
     */
    public fun rexSubqueryComp(
        args: List<Rex>,
        comp: RexSubqueryComp.Comp,
        rel: Rel,
    ): RexSubqueryComp = RexSubqueryCompImpl(args, comp, null, rel)

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
     * @param table
     * @return
     */
    public fun rexTable(table: Table): RexTable = RexTableImpl(table)
}
