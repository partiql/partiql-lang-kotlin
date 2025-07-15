package org.partiql.eval.internal.compiler

import org.partiql.eval.Environment
import org.partiql.eval.Expr
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.compiler.Match
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.eval.compiler.Strategy
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.eval.internal.operator.rel.RelOpAggregate
import org.partiql.eval.internal.operator.rel.RelOpDistinct
import org.partiql.eval.internal.operator.rel.RelOpExceptAll
import org.partiql.eval.internal.operator.rel.RelOpExceptDistinct
import org.partiql.eval.internal.operator.rel.RelOpExclude
import org.partiql.eval.internal.operator.rel.RelOpFilter
import org.partiql.eval.internal.operator.rel.RelOpIntersectAll
import org.partiql.eval.internal.operator.rel.RelOpIntersectDistinct
import org.partiql.eval.internal.operator.rel.RelOpIterate
import org.partiql.eval.internal.operator.rel.RelOpIteratePermissive
import org.partiql.eval.internal.operator.rel.RelOpJoinInner
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterLeft
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterRight
import org.partiql.eval.internal.operator.rel.RelOpLimit
import org.partiql.eval.internal.operator.rel.RelOpOffset
import org.partiql.eval.internal.operator.rel.RelOpProject
import org.partiql.eval.internal.operator.rel.RelOpScan
import org.partiql.eval.internal.operator.rel.RelOpScanPermissive
import org.partiql.eval.internal.operator.rel.RelOpSort
import org.partiql.eval.internal.operator.rel.RelOpUnionAll
import org.partiql.eval.internal.operator.rel.RelOpUnionDistinct
import org.partiql.eval.internal.operator.rel.RelOpUnpivot
import org.partiql.eval.internal.operator.rex.ExprArray
import org.partiql.eval.internal.operator.rex.ExprBag
import org.partiql.eval.internal.operator.rex.ExprCall
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCaseBranch
import org.partiql.eval.internal.operator.rex.ExprCaseSearched
import org.partiql.eval.internal.operator.rex.ExprCast
import org.partiql.eval.internal.operator.rex.ExprCoalesce
import org.partiql.eval.internal.operator.rex.ExprError
import org.partiql.eval.internal.operator.rex.ExprLit
import org.partiql.eval.internal.operator.rex.ExprMissing
import org.partiql.eval.internal.operator.rex.ExprNullIf
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPivotPermissive
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprSpread
import org.partiql.eval.internal.operator.rex.ExprStructField
import org.partiql.eval.internal.operator.rex.ExprStructPermissive
import org.partiql.eval.internal.operator.rex.ExprStructStrict
import org.partiql.eval.internal.operator.rex.ExprSubquery
import org.partiql.eval.internal.operator.rex.ExprSubqueryRow
import org.partiql.eval.internal.operator.rex.ExprTable
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.plan.Action
import org.partiql.plan.Collation
import org.partiql.plan.JoinType
import org.partiql.plan.Operand
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
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
import org.partiql.plan.rel.RelWith
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
import org.partiql.spi.Context
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * This class is responsible for producing an executable statement from logical operators.
 */
internal class StandardCompiler(strategies: List<Strategy>) : PartiQLCompiler {

    private val strategies: List<Strategy> = strategies

    internal constructor() : this(emptyList())

    override fun prepare(plan: Plan, mode: Mode, ctx: Context): Statement {
        try {
            val visitor = Visitor(mode)
            val operation = plan.action
            val statement: Statement = when {
                operation is Action.Query -> visitor.compile(operation)
                else -> throw IllegalArgumentException("Only query statements are supported")
            }
            return statement
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            val error = PError.INTERNAL_ERROR(PErrorKind.COMPILATION(), null, t)
            ctx.errorListener.report(error)
            return Statement { Datum.missing() }
        }
    }

    /**
     * Transforms plan relation operators into the internal physical operators.
     */
    private inner class Visitor(mode: Mode) : OperatorVisitor<Expr, Unit> {

        private val mode = mode
        private val MODE = mode.code()

        /**
         * Compile a query operation to a query statement.
         */
        fun compile(action: Action.Query) = object : Statement {

            // compile the query root
            private val root = compile(action.getRex(), Unit).catch()

            // execute with no parameters
            override fun execute(): Datum {
                return try {
                    root.eval(Environment())
                } catch (e: PRuntimeException) {
                    throw e
                } catch (t: Throwable) {
                    throw PErrors.internalErrorException(t)
                }
            }
        }

        /**
         * Apply custom strategies left-to-right, returning the first match.
         *
         * This is very simple because I am trying to leverage the current visitor at the moment for a small diff.
         *
         * @param operator
         * @return
         */
        private fun compileWithStrategies(operator: Operator): Expr {
            // if strategy matches root, compile children to form a match.
            for (strategy in strategies) {
                // first match
                if (strategy.pattern.matches(operator)) {
                    // assume single match
                    val operand = Operand.single(operator)
                    val match = Match(operand)
                    return strategy.apply(match, mode, ::compileWithStrategies)
                }
            }
            return operator.accept(this, Unit)
        }

        // TODO REMOVE ME
        private fun compile(rel: Rel, ctx: Unit): ExprRelation = compileWithStrategies(rel) as ExprRelation

        // TODO REMOVE ME
        private fun compile(rex: Rex, ctx: Unit): ExprValue = compileWithStrategies(rex) as ExprValue

        override fun defaultReturn(operator: Operator, ctx: Unit): Expr {
            error("No compiler strategy matches the operator: ${operator::class.java.simpleName}")
        }

        // OPERATORS

        override fun visitAggregate(rel: RelAggregate, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val aggs = rel.getMeasures().map { call ->
                val agg = call.getAgg()
                val args = call.getArgs().map { compile(it, ctx).catch() }
                val distinct = call.isDistinct()
                Aggregate(agg, args, distinct)
            }
            val groups = rel.getGroups().map { compile(it, ctx).catch() }
            return RelOpAggregate(input, aggs, groups)
        }

        override fun visitDistinct(rel: RelDistinct, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            return RelOpDistinct(input)
        }

        override fun visitExcept(rel: RelExcept, ctx: Unit): ExprRelation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpExceptAll(lhs, rhs)
                else -> RelOpExceptDistinct(lhs, rhs)
            }
        }

        override fun visitExclude(rel: RelExclude, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val paths = rel.getExclusions()
            return RelOpExclude(input, paths)
        }

        override fun visitFilter(rel: RelFilter, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val predicate = compile(rel.getPredicate(), ctx).catch()
            return RelOpFilter(input, predicate)
        }

        override fun visitIntersect(rel: RelIntersect, ctx: Unit): ExprRelation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpIntersectAll(lhs, rhs)
                else -> RelOpIntersectDistinct(lhs, rhs)
            }
        }

        override fun visitIterate(rel: RelIterate, ctx: Unit): ExprRelation {
            val input = compile(rel.getRex(), ctx)
            return when (MODE) {
                Mode.PERMISSIVE -> RelOpIteratePermissive(input)
                Mode.STRICT -> RelOpIterate(input)
                else -> throw IllegalStateException("Unsupported execution mode: $MODE")
            }
        }

        override fun visitJoin(rel: RelJoin, ctx: Unit): ExprRelation {
            val lrel = rel.left
            val rrel = rel.right
            val lhs = compile(lrel, ctx)
            val rhs = compile(rrel, ctx)
            val condition = compile(rel.getCondition(), ctx)
            // use schema for null padding
            val lhsType = lrel.type
            val rhsType = rrel.type
            return when (rel.joinType.code()) {
                JoinType.INNER -> RelOpJoinInner(lhs, rhs, condition)
                JoinType.LEFT -> RelOpJoinOuterLeft(lhs, rhs, condition, rhsType)
                JoinType.RIGHT -> RelOpJoinOuterRight(lhs, rhs, condition, lhsType)
                JoinType.FULL -> RelOpJoinOuterFull(lhs, rhs, condition, lhsType, rhsType)
                else -> error("Unsupported join type: ${rel.joinType}")
            }
        }

        override fun visitLimit(rel: RelLimit, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val limit = compile(rel.getLimit(), ctx)
            return RelOpLimit(input, limit)
        }

        override fun visitOffset(rel: RelOffset, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val offset = compile(rel.getOffset(), ctx)
            return RelOpOffset(input, offset)
        }

        override fun visitProject(rel: RelProject, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val projections = rel.getProjections().map { compile(it, ctx).catch() }
            return RelOpProject(input, projections)
        }

        override fun visitScan(rel: RelScan, ctx: Unit): ExprRelation {
            val input = compile(rel.rex, ctx)
            return when (MODE) {
                Mode.PERMISSIVE -> RelOpScanPermissive(input)
                Mode.STRICT -> RelOpScan(input)
                else -> throw IllegalStateException("Unsupported execution mode: $MODE")
            }
        }

        override fun visitSort(rel: RelSort, ctx: Unit): ExprRelation {
            val input = compile(rel.getInput(), ctx)
            val collations = rel.getCollations().map {
                val expr = compile(it.column, ctx)
                val desc = it.order.code() == Collation.Order.DESC
                val last = it.nulls.code() == Collation.Nulls.LAST
                RelOpSort.Collation(expr, desc, last)
            }
            return RelOpSort(input, collations)
        }

        override fun visitUnion(rel: RelUnion, ctx: Unit): ExprRelation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpUnionAll(lhs, rhs)
                else -> RelOpUnionDistinct(lhs, rhs)
            }
        }

        override fun visitUnpivot(rel: RelUnpivot, ctx: Unit): ExprRelation {
            val input = compile(rel.rex, ctx)
            return when (MODE) {
                Mode.PERMISSIVE -> RelOpUnpivot.Permissive(input)
                Mode.STRICT -> RelOpUnpivot.Strict(input)
                else -> throw IllegalStateException("Unsupported execution mode: $MODE")
            }
        }

        override fun visitWith(rel: RelWith, ctx: Unit): ExprRelation {
            // Rewrite to not include the `RelWith` (assumes the planner rewrote WITH variable references to the query
            // representation)
            return compile(rel.input, ctx)
        }

        override fun visitError(rex: RexError, ctx: Unit): ExprValue {
            return when (mode.code()) {
                Mode.PERMISSIVE -> ExprMissing(PType.unknown())
                Mode.STRICT -> ExprError()
                else -> throw IllegalStateException("Unsupported execution mode: $mode")
            }
        }

        // OPERATORS

        override fun visitArray(rex: RexArray, ctx: Unit): ExprValue {
            val values = rex.getValues().map { compile(it, ctx).catch() }
            return ExprArray(values)
        }

        override fun visitBag(rex: RexBag, ctx: Unit): ExprValue {
            val values = rex.getValues().map { compile(it, ctx).catch() }
            return ExprBag(values)
        }

        override fun visitDispatch(rex: RexDispatch, ctx: Unit): ExprValue {
            // Check candidate arity for uniformity
            var arity: Int = -1
            val name = rex.getName()
            // Check the candidate list size
            val functions = rex.getFunctions()
            if (functions.isEmpty()) {
                error("Dynamic call had an empty candidates list: $rex.")
            }
            // Compile the candidates
            val candidates = Array(functions.size) {
                val fn = functions[it]
                val fnArity = fn.signature.arity
                if (arity == -1) {
                    // set first
                    arity = fnArity
                } else {
                    if (fnArity != arity) {
                        error("Dynamic call candidate had different arity than others; found $fnArity but expected $arity")
                    }
                }
                // make a candidate
                fn
            }
            val args = rex.getArgs().map { compile(it, ctx).catch() }.toTypedArray()
            return ExprCallDynamic(name, candidates, args)
        }

        override fun visitCall(rex: RexCall, ctx: Unit): ExprValue {
            val func = rex.getFunction()
            val args = rex.getArgs()
            val catch = func.signature.parameters.any { it.type.code() == PType.DYNAMIC }
            return when (catch) {
                true -> ExprCall(func, Array(args.size) { i -> compile(args[i], Unit).catch() })
                else -> ExprCall(func, Array(args.size) { i -> compile(args[i], Unit) })
            }
        }

        override fun visitCase(rex: RexCase, ctx: Unit): ExprValue {
            if (rex.getMatch() != null) {
                TODO("<case> expression")
            }
            val branches = rex.getBranches().map {
                val value = compile(it.getCondition(), ctx).catch()
                val result = compile(it.getResult(), ctx)
                ExprCaseBranch(value, result)
            }
            val default = rex.getDefault()?.let { compile(it, ctx) }
            return ExprCaseSearched(branches, default)
        }

        override fun visitCast(rex: RexCast, ctx: Unit): ExprValue {
            val operand = compile(rex.getOperand(), ctx)
            val target = rex.getTarget()
            return ExprCast(operand, target)
        }

        override fun visitCoalesce(rex: RexCoalesce, ctx: Unit): ExprValue {
            val args = rex.getArgs().map { compile(it, ctx) }.toTypedArray()
            return ExprCoalesce(args)
        }

        override fun visitLit(rex: RexLit, ctx: Unit): ExprValue {
            return ExprLit(rex.getDatum())
        }

        override fun visitNullIf(rex: RexNullIf, ctx: Unit): ExprValue {
            val value = compile(rex.getV1(), ctx)
            val nullifier = compile(rex.getV2(), ctx)
            return ExprNullIf(value, nullifier)
        }

        override fun visitPathIndex(rex: RexPathIndex, ctx: Unit): ExprValue {
            val operand = compile(rex.getOperand(), ctx)
            val index = compile(rex.getIndex(), ctx)
            return ExprPathIndex(operand, index)
        }

        override fun visitPathKey(rex: RexPathKey, ctx: Unit): ExprValue {
            val operand = compile(rex.getOperand(), ctx)
            val key = compile(rex.getKey(), ctx)
            return ExprPathKey(operand, key)
        }

        override fun visitPathSymbol(rex: RexPathSymbol, ctx: Unit): ExprValue {
            val operand = compile(rex.getOperand(), ctx)
            val symbol = rex.getSymbol()
            return ExprPathSymbol(operand, symbol)
        }

        override fun visitPivot(rex: RexPivot, ctx: Unit): ExprValue {
            val input = compile(rex.getInput(), ctx)
            val key = compile(rex.getKey(), ctx)
            val value = compile(rex.getValue(), ctx)
            return when (MODE) {
                Mode.PERMISSIVE -> ExprPivotPermissive(input, key, value)
                Mode.STRICT -> ExprPivot(input, key, value)
                else -> throw IllegalStateException("Unsupported execution mode: $MODE")
            }
        }

        override fun visitSelect(rex: RexSelect, ctx: Unit): ExprValue {
            val input = compile(rex.getInput(), ctx)
            val constructor = compile(rex.getConstructor(), ctx).catch()
            val ordered = rex.getInput().type.isOrdered
            return ExprSelect(input, constructor, ordered)
        }

        override fun visitStruct(rex: RexStruct, ctx: Unit): ExprValue {
            val fields = rex.getFields().map {
                val k = compile(it.key, ctx)
                val v = compile(it.value, ctx).catch()
                ExprStructField(k, v)
            }
            return when (MODE) {
                Mode.PERMISSIVE -> ExprStructPermissive(fields)
                Mode.STRICT -> ExprStructStrict(fields)
                else -> throw IllegalStateException("Unsupported execution mode: $MODE")
            }
        }

        override fun visitSubquery(rex: RexSubquery, ctx: Unit): ExprValue {
            val rel = compile(rex.getInput(), ctx)
            val constructor = compile(rex.getConstructor(), ctx)
            return when (rex.isScalar()) {
                true -> ExprSubquery(rel, constructor)
                else -> ExprSubqueryRow(rel, constructor)
            }
        }

        override fun visitSubqueryComp(rex: RexSubqueryComp, ctx: Unit): ExprValue {
            TODO("<exists predicate> and <unique predicate>")
        }

        override fun visitSubqueryIn(rex: RexSubqueryIn, ctx: Unit): ExprValue {
            TODO("<in predicate>")
        }

        override fun visitSubqueryTest(rex: RexSubqueryTest, ctx: Unit): ExprValue {
            TODO("<exists predicate> and <unique predicate>")
        }

        override fun visitSpread(rex: RexSpread, ctx: Unit): ExprValue {
            val args = rex.getArgs().map { compile(it, ctx) }.toTypedArray()
            return ExprSpread(args)
        }

        override fun visitTable(rex: RexTable, ctx: Unit): ExprValue {
            return ExprTable(rex.getTable())
        }

        override fun visitVar(rex: RexVar, ctx: Unit): ExprValue {
            val scope = rex.scope
            val offset = rex.getOffset()
            return ExprVar(scope, offset)
        }

        /**
         * Some places "catch" an error and return the MISSING value.
         */
        private fun ExprValue.catch(): ExprValue = when (MODE) {
            Mode.PERMISSIVE -> ExprPermissive(this)
            Mode.STRICT -> this
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }
}
