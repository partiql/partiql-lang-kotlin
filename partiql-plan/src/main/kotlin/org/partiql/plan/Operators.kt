package org.partiql.plan

import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelAggregate.Measure
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelDistinct
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rel.RelUnpivot
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexError
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexSpread
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexSubqueryComp
import org.partiql.plan.rex.RexSubqueryIn
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Function
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * The [Operators] interface is a factory to instantiate operator implementations.
 *
 * There is only ONE factory method per operators – i.e. no overloads or defaults.
 */
public interface Operators {

    public companion object {

        /**
         * A singleton of the [Operators] with all the standard implementations.
         */
        @JvmField
        public val STANDARD: Operators = object : Operators {}
    }

    // --- REL OPERATORS -----------------------------------------------------------------------------------------------

    /**
     * Create a [RelAggregate] instance.
     *
     * @param input
     * @param measures
     * @param groups
     *
     * @param input
     */
    public fun aggregate(
        input: Rel,
        measures: List<Measure>,
        groups: List<Rex>,
    ): RelAggregate = RelAggregate.create(input, measures, groups)

    /**
     * **Note**: This is experimental and subject to change without prior notice!
     *
     * Create a [RelCorrelate] instance for a lateral join or correlated subquery.
     *
     * @param lhs
     * @param rhs
     * @param joinType
     * @return
     */
    public fun correlate(
        lhs: Rel,
        rhs: Rel,
        joinType: JoinType,
    ): RelCorrelate = RelCorrelate.create(lhs, rhs, joinType)

    /**
     * Create a [RelDistinct] instance.
     *
     * @param input
     * @return
     */
    public fun distinct(input: Rel): RelDistinct = RelDistinct.create(input)

    /**
     * Create a [RelExcept] instance for EXCEPT [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun except(
        lhs: Rel,
        rhs: Rel,
        all: Boolean,
    ): RelExcept = RelExcept.create(lhs, rhs, all)

    /**
     * Create a [RelExclude] instance.
     *
     * @param input
     * @param exclusions
     * @return
     */
    public fun exclude(input: Rel, exclusions: List<Exclusion>): RelExclude = RelExclude.create(input, exclusions)

    /**
     * Create a [RelFilter] instance.
     *
     * @param input
     * @param predicate
     * @return
     */
    public fun filter(input: Rel, predicate: Rex): RelFilter = RelFilter.create(input, predicate)

    /**
     * Create a [RelIntersect] instance for INTERSECT [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @param all
     * @return
     */
    public fun intersect(
        lhs: Rel,
        rhs: Rel,
        all: Boolean,
    ): RelIntersect = RelIntersect.create(lhs, rhs, all)

    /**
     * Create a [RelIterate] instance.
     *
     * @param rex
     * @return
     */
    public fun iterate(rex: Rex): RelIterate = RelIterate.create(rex)

    /**
     * Create a [RelJoin] instance.
     *
     * @param lhs
     * @param rhs
     * @param condition
     * @param type
     * @return
     */
    public fun join(
        lhs: Rel,
        rhs: Rel,
        condition: Rex,
        type: JoinType,
    ): RelJoin = RelJoin.create(lhs, rhs, condition, type)

    /**
     * Create a [RelLimit] instance.
     *
     * @param input
     * @param limit
     * @return
     */
    public fun limit(input: Rel, limit: Rex): RelLimit = RelLimit.create(input, limit)

    /**
     * Create a [RelOffset] instance.
     *
     * @param input
     * @param offset
     * @return
     */
    public fun offset(input: Rel, offset: Rex): RelOffset = RelOffset.create(input, offset)

    /**
     * Create a [RelProject] instance.
     *
     * @param input
     * @param projections
     * @return
     */
    public fun project(input: Rel, projections: List<Rex>): RelProject = RelProject.create(input, projections)

    /**
     * Create a [RelScan] instance.
     *
     * @param rex
     * @return
     */
    public fun scan(rex: Rex): RelScan = RelScan.create(rex)

    /**
     * Create a [RelSort] instance.
     *
     * @param input
     * @param collations
     * @return
     */
    public fun sort(input: Rel, collations: List<Collation>): RelSort = RelSort.create(input, collations)

    /**
     * Create a [RelUnion] instance for UNION [ALL|DISTINCT].
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public fun union(lhs: Rel, rhs: Rel, all: Boolean): RelUnion = RelUnion.create(lhs, rhs, all)

    /**
     * Create a [RelUnpivot] instance.
     *
     * @param rex
     * @return
     */
    public fun unpivot(rex: Rex): RelUnpivot = RelUnpivot.create(rex)

    // --- REX OPERATORS -----------------------------------------------------------------------------------------------

    /**
     * Create a [RexArray] instance.
     *
     * @param values
     * @return
     */
    public fun array(values: List<Rex>): RexArray = RexArray.create(values)

    /**
     * Create a [RexBag] instance.
     *
     * @param values
     * @return
     */
    public fun bag(values: Collection<Rex>): RexBag = RexBag.create(values)

    /**
     * Create a [RexCall] instance.
     *
     * @param function
     * @param args
     * @return
     */
    public fun call(function: Function.Instance, args: List<Rex>): RexCall = RexCall.create(function, args)

    /**
     * Create a [RexCase] instance for a case-when with dynamic type (case is a reserved word in Java).
     *
     * @param match
     * @param branches
     * @param default
     * @return
     */
    public fun caseWhen(match: Rex?, branches: List<RexCase.Branch>, default: Rex?): RexCase =
        RexCase.create(match, branches, default)

    /**
     * Create a [RexCast] instance.
     *
     * @param operand
     * @param target
     * @return
     */
    public fun cast(operand: Rex, target: PType): RexCast = RexCast.create(operand, target)

    /**
     * Create a [RexCoalesce] instance.
     *
     * @param args
     * @return
     */
    public fun coalesce(args: List<Rex>): RexCoalesce = RexCoalesce.create(args)

    /**
     * Create a [RexDispatch] instance.
     *
     * @param name
     * @param functions
     * @param args
     * @return
     */
    public fun dispatch(name: String, functions: List<Function>, args: List<Rex>): RexDispatch =
        RexDispatch.create(name, functions, args)

    /**
     * Create a [RexError] instance.
     */
    public fun error(type: PType): RexError = RexError.create()

    /**
     * Create a [RexLit] instance.
     *
     * @param value
     * @return
     */
    public fun lit(value: Datum): RexLit = RexLit.create(value)

    /**
     * Create a [RexNullIf] instance.
     */
    public fun nullIf(v1: Rex, v2: Rex): RexNullIf = RexNullIf.create(v1, v2)

    /**
     * Create a [RexPathIndex] instance.
     *
     * @param operand
     * @param index
     * @return
     */
    public fun pathIndex(operand: Rex, index: Rex): RexPathIndex = RexPathIndex.create(operand, index)

    /**
     * Create a [RexPathKey] instance.
     *
     * @param operand
     * @param key
     * @return
     */
    public fun pathKey(operand: Rex, key: Rex): RexPathKey = RexPathKey.create(operand, key)

    /**
     * Create a [RexPathSymbol] instance.
     *
     * @param operand
     * @param symbol
     * @return
     */
    public fun pathSymbol(operand: Rex, symbol: String): RexPathSymbol = RexPathSymbol.create(operand, symbol)

    /**
     * Create a [RexPivot] instance.
     *
     * @param input
     * @param key
     * @param value
     * @return
     */
    public fun pivot(input: Rel, key: Rex, value: Rex): RexPivot = RexPivot.create(input, key, value)

    /**
     * Create a [RexSelect] instance.
     *
     * @param input
     * @param constructor
     * @return
     */
    public fun select(input: Rel, constructor: Rex): RexSelect = RexSelect.create(input, constructor)

    /**
     * Create a [RexSpread] instance with open struct type.
     *
     * @param args
     * @return
     */
    public fun spread(args: List<Rex>): RexSpread = RexSpread.create(args)

    /**
     * Create a [RexStruct] instance.
     *
     * @param fields
     * @return
     */
    public fun struct(fields: List<RexStruct.Field>): RexStruct = RexStruct.create(fields)

    /**
     * Create a [RexSubquery] instance.
     *
     * TODO REMOVE constructor AND scalar – TEMPORARY UNTIL SUBQUERIES ARE FIXED IN THE PLANNER.
     *
     * @param input
     * @return
     */
    public fun subquery(input: Rel, constructor: Rex, scalar: Boolean): RexSubquery =
        RexSubquery.create(input, constructor, scalar)

    /**
     * Create a [RexSubqueryComp] instance.
     *
     * @param input
     * @param args
     * @param comparison
     * @param quantifier
     * @return
     */
    public fun subqueryComp(
        input: Rel,
        args: List<Rex>,
        comparison: RexSubqueryComp.Comparison,
        quantifier: RexSubqueryComp.Quantifier,
    ): RexSubqueryComp = RexSubqueryComp.create(input, args, comparison, quantifier)

    /**
     * Create a [RexSubqueryIn] instance for a list argument.
     *
     * @param input
     * @param args
     * @return
     */
    public fun subqueryIn(input: Rel, args: List<Rex>): RexSubqueryIn = RexSubqueryIn.create(input, args)

    /**
     * Create a [RexSubqueryTest] instance.
     *
     * @param input
     * @param test
     * @return
     */
    public fun subqueryTest(input: Rel, test: RexSubqueryTest.Test): RexSubqueryTest =
        RexSubqueryTest.create(input, test)

    /**
     * Create a [RexTable] instance.
     *
     * @param table
     * @return
     */
    public fun table(table: Table): RexTable = RexTable.create(table)

    /**
     * Create a [RexVar] instance (requires a type).
     *
     * @param depth
     * @param offset
     * @param type
     * @return
     */
    public fun variable(depth: Int, offset: Int, type: PType): RexVar = RexVar.create(depth, offset, type)
}
