package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelOpAggregate
import org.partiql.eval.internal.operator.rel.RelOpDistinct
import org.partiql.eval.internal.operator.rel.RelOpExceptAll
import org.partiql.eval.internal.operator.rel.RelOpExceptDistinct
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
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCallStatic
import org.partiql.eval.internal.operator.rex.ExprCaseBranch
import org.partiql.eval.internal.operator.rex.ExprCaseSearched
import org.partiql.eval.internal.operator.rex.ExprCast
import org.partiql.eval.internal.operator.rex.ExprCoalesce
import org.partiql.eval.internal.operator.rex.ExprLit
import org.partiql.eval.internal.operator.rex.ExprMissing
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStructField
import org.partiql.eval.internal.operator.rex.ExprStructPermissive
import org.partiql.eval.internal.operator.rex.ExprStructStrict
import org.partiql.eval.internal.operator.rex.ExprSubquery
import org.partiql.eval.internal.operator.rex.ExprSubqueryRow
import org.partiql.eval.internal.operator.rex.ExprTable
import org.partiql.eval.internal.operator.rex.ExprTupleUnion
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.eval.value.Datum
import org.partiql.plan.relType
import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.plan.v1.operator.rel.RelAggregate
import org.partiql.plan.v1.operator.rel.RelCollation
import org.partiql.plan.v1.operator.rel.RelDistinct
import org.partiql.plan.v1.operator.rel.RelError
import org.partiql.plan.v1.operator.rel.RelExcept
import org.partiql.plan.v1.operator.rel.RelExclude
import org.partiql.plan.v1.operator.rel.RelFilter
import org.partiql.plan.v1.operator.rel.RelIntersect
import org.partiql.plan.v1.operator.rel.RelIterate
import org.partiql.plan.v1.operator.rel.RelJoin
import org.partiql.plan.v1.operator.rel.RelJoinType
import org.partiql.plan.v1.operator.rel.RelLimit
import org.partiql.plan.v1.operator.rel.RelOffset
import org.partiql.plan.v1.operator.rel.RelProject
import org.partiql.plan.v1.operator.rel.RelScan
import org.partiql.plan.v1.operator.rel.RelSort
import org.partiql.plan.v1.operator.rel.RelUnion
import org.partiql.plan.v1.operator.rel.RelUnpivot
import org.partiql.plan.v1.operator.rel.RelVisitor
import org.partiql.plan.v1.operator.rex.Rex
import org.partiql.plan.v1.operator.rex.RexArray
import org.partiql.plan.v1.operator.rex.RexBag
import org.partiql.plan.v1.operator.rex.RexCallDynamic
import org.partiql.plan.v1.operator.rex.RexCallStatic
import org.partiql.plan.v1.operator.rex.RexCase
import org.partiql.plan.v1.operator.rex.RexCast
import org.partiql.plan.v1.operator.rex.RexCoalesce
import org.partiql.plan.v1.operator.rex.RexError
import org.partiql.plan.v1.operator.rex.RexLit
import org.partiql.plan.v1.operator.rex.RexMissing
import org.partiql.plan.v1.operator.rex.RexPathIndex
import org.partiql.plan.v1.operator.rex.RexPathKey
import org.partiql.plan.v1.operator.rex.RexPathSymbol
import org.partiql.plan.v1.operator.rex.RexPivot
import org.partiql.plan.v1.operator.rex.RexSelect
import org.partiql.plan.v1.operator.rex.RexSpread
import org.partiql.plan.v1.operator.rex.RexStruct
import org.partiql.plan.v1.operator.rex.RexSubquery
import org.partiql.plan.v1.operator.rex.RexSubqueryComp
import org.partiql.plan.v1.operator.rex.RexSubqueryIn
import org.partiql.plan.v1.operator.rex.RexSubqueryTest
import org.partiql.plan.v1.operator.rex.RexTable
import org.partiql.plan.v1.operator.rex.RexVar
import org.partiql.plan.v1.operator.rex.RexVisitor
import org.partiql.planner.catalog.Session
import org.partiql.types.PType

/**
 * See https://github.com/partiql/partiql-lang-kotlin/blob/v1/partiql-eval/src/main/kotlin/org/partiql/eval/internal/Compiler.kt
 */
internal class SqlCompiler(
    @JvmField var mode: PartiQLEngine.Mode,
    @JvmField var session: Session,
) {

    private val relCompiler = RelCompiler()

    private val rexCompiler = RexCompiler()

    fun compile(rel: Rel, ctx: Unit): Operator.Relation = rel.accept(relCompiler, ctx)

    fun compile(rex: Rex, ctx: Unit): Operator.Expr = rex.accept(rexCompiler, ctx)

    /**
     * Transforms plan relation operators into the internal physical operators.
     */
    private inner class RelCompiler : RelVisitor<Operator.Relation, Unit> {

        override fun defaultReturn(rel: Rel, ctx: Unit): Operator.Relation {
            TODO("Evaluation is not implemented for rel: ${rel::class.simpleName}")
        }

        override fun visitError(rel: RelError, ctx: Unit): Operator.Relation {
            throw IllegalStateException(rel.message)
        }

        // OPERATORS

        override fun visitAggregate(rel: RelAggregate, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val keys = rel.getGroups().map { compile(it, ctx).catch() }
            val aggs = rel.getCalls().map { TODO() }
            return RelOpAggregate(input, keys, aggs)
        }

        override fun visitDistinct(rel: RelDistinct, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return RelOpDistinct(input)
        }

        override fun visitExcept(rel: RelExcept, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpExceptAll(lhs, rhs)
                else -> RelOpExceptDistinct(lhs, rhs)
            }
        }

        override fun visitExclude(rel: RelExclude, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            TODO("EXCLUDE paths in compiler")
        }

        override fun visitFilter(rel: RelFilter, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val predicate = compile(rel.getPredicate(), ctx).catch()
            return RelOpFilter(input, predicate)
        }

        override fun visitIntersect(rel: RelIntersect, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpIntersectAll(lhs, rhs)
                else -> RelOpIntersectDistinct(lhs, rhs)
            }
        }

        override fun visitIterate(rel: RelIterate, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpIteratePermissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpIterate(input)
            }
        }

        override fun visitJoin(rel: RelJoin, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            val condition = rel.getCondition()?.let { compile(it, ctx) } ?: ExprLit(Datum.bool(true))

            // TODO JOIN SCHEMAS
            val lhsType = relType(emptyList(), emptySet())
            val rhsType = relType(emptyList(), emptySet())

            return when (rel.getJoinType()) {
                RelJoinType.INNER -> RelOpJoinInner(lhs, rhs, condition)
                RelJoinType.LEFT -> RelOpJoinOuterLeft(lhs, rhs, condition, rhsType)
                RelJoinType.RIGHT -> RelOpJoinOuterRight(lhs, rhs, condition, lhsType)
                RelJoinType.FULL -> RelOpJoinOuterFull(lhs, rhs, condition, lhsType, rhsType)
            }
        }

        override fun visitLimit(rel: RelLimit, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val limit = compile(rel.getLimit(), ctx)
            return RelOpLimit(input, limit)
        }

        override fun visitOffset(rel: RelOffset, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val offset = compile(rel.getOffset(), ctx)
            return RelOpOffset(input, offset)
        }

        override fun visitProject(rel: RelProject, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val projections = rel.getProjections().map { compile(it, ctx).catch() }
            return RelOpProject(input, projections)
        }

        override fun visitScan(rel: RelScan, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpScanPermissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpScan(input)
            }
        }

        override fun visitSort(rel: RelSort, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val collations = rel.getCollations().map {
                val expr = compile(it.getRex(), ctx)
                val desc = it.getOrder() == RelCollation.Order.DESC
                val last = it.getNulls() == RelCollation.Nulls.LAST
                RelOpSort.Collation(expr, desc, last)
            }
            return RelOpSort(input, collations)
        }

        override fun visitUnion(rel: RelUnion, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpUnionAll(lhs, rhs)
                else -> RelOpUnionDistinct(lhs, rhs)
            }
        }

        override fun visitUnpivot(rel: RelUnpivot, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpUnpivot.Permissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpUnpivot.Strict(input)
            }
        }
    }

    /**
     * Transforms plan expression operators into the internal physical expressions.
     */
    private inner class RexCompiler : RexVisitor<Operator.Expr, Unit> {

        //
        private val unknown = PType.unknown()

        override fun defaultReturn(rex: Rex, ctx: Unit): Operator.Expr {
            TODO("Not yet implemented")
        }

        override fun visitError(rex: RexError, ctx: Unit): Operator.Expr {
            throw IllegalStateException(rex.getMessage())
        }

        // OPERATORS

        override fun visitArray(rex: RexArray, ctx: Unit): Operator.Expr {
            val values = rex.getValues().map { compile(it, ctx).catch() }
            return ExprArray(values)
        }

        override fun visitBag(rex: RexBag, ctx: Unit): Operator.Expr {
            val values = rex.getValues().map { compile(it, ctx).catch() }
            return ExprBag(values)
        }

        override fun visitCallDynamic(rex: RexCallDynamic, ctx: Unit): Operator.Expr {
            // Check candidate name and arity for uniformity
            var arity: Int = -1
            var name = "unknown"
            // Check the candidate list size
            val functions = rex.getFunctions()
            if (functions.isEmpty()) {
                error("Rex.Op.Call.Dynamic had an empty candidates list: $rex.")
            }
            // Compile the candidates
            val candidates = Array(functions.size) {
                val fn = functions[it]
                val fnArity = fn.signature.parameters.size
                val fnName = fn.signature.name.uppercase()
                if (arity == -1) {
                    arity = fnArity
                    name = fnName
                } else {
                    if (fnArity != arity) {
                        error("Dynamic call candidate had different arity than others; found $fnArity but expected $arity")
                    }
                    if (fnName != name) {
                        error("Dynamic call candidate had different name than others; found $fnName but expected $name")
                    }
                }
                fn
            }
            val args = rex.getArgs().map { compile(it, ctx).catch() }.toTypedArray()
            return ExprCallDynamic(name, candidates, args)
        }

        override fun visitCallStatic(rex: RexCallStatic, ctx: Unit): Operator.Expr {
            val fn = rex.getFunction()
            val args = rex.getArgs().map { compile(it, ctx) }
            val fnTakesInMissing = fn.signature.parameters.any {
                it.type.kind == PType.Kind.DYNAMIC // TODO: Is this needed?
            }
            return when (fnTakesInMissing) {
                true -> ExprCallStatic(fn, args.map { it.catch() }.toTypedArray())
                else -> ExprCallStatic(fn, args.toTypedArray())
            }
        }

        override fun visitCase(rex: RexCase, ctx: Unit): Operator.Expr {
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

        override fun visitCast(rex: RexCast, ctx: Unit): Operator.Expr {
            val operand = compile(rex.getOperand(), ctx)
            val target = rex.getTarget()
            return ExprCast(operand, target)
        }

        override fun visitCoalesce(rex: RexCoalesce, ctx: Unit): Operator.Expr {
            val args = rex.getArgs().map { compile(it, ctx) }.toTypedArray()
            return ExprCoalesce(args)
        }

        override fun visitLit(rex: RexLit, ctx: Unit): Operator.Expr {
            return ExprLit(rex.getValue())
        }

        override fun visitMissing(rex: RexMissing, ctx: Unit): Operator.Expr {
            return ExprMissing(unknown)
        }

        override fun visitPathIndex(rex: RexPathIndex, ctx: Unit): Operator.Expr {
            val operand = compile(rex.getOperand(), ctx)
            val index = compile(rex.getIndex(), ctx)
            return ExprPathIndex(operand, index)
        }

        override fun visitPathKey(rex: RexPathKey, ctx: Unit): Operator.Expr {
            val operand = compile(rex.getOperand(), ctx)
            val key = compile(rex.getKey(), ctx)
            return ExprPathKey(operand, key)
        }

        override fun visitPathSymbol(rex: RexPathSymbol, ctx: Unit): Operator.Expr {
            val operand = compile(rex.getOperand(), ctx)
            val symbol = rex.getSymbol()
            return ExprPathSymbol(operand, symbol)
        }

        override fun visitPivot(rex: RexPivot, ctx: Unit): Operator.Expr {
            val input = compile(rex.getInput(), ctx)
            val key = compile(rex.getKey(), ctx)
            val value = compile(rex.getValue(), ctx)
            return ExprPivot(input, key, value)
        }

        override fun visitSelect(rex: RexSelect, ctx: Unit): Operator.Expr {
            val input = compile(rex.getInput(), ctx)
            val constructor = compile(rex.getConstructor(), ctx).catch()
            val ordered = rex.getInput().isOrdered()
            return ExprSelect(input, constructor, ordered)
        }

        override fun visitStruct(rex: RexStruct, ctx: Unit): Operator.Expr {
            val fields = rex.getFields().map {
                val k = compile(it.getKey(), ctx)
                val v = compile(it.getValue(), ctx).catch()
                ExprStructField(k, v)
            }
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> ExprStructPermissive(fields)
                PartiQLEngine.Mode.STRICT -> ExprStructStrict(fields)
            }
        }

        override fun visitSubquery(rex: RexSubquery, ctx: Unit): Operator.Expr {
            val rel = compile(rex.getRel(), ctx)
            val constructor = compile(rex.getConstructor(), ctx)
            return when (rex.asScalar()) {
                true -> ExprSubquery(rel, constructor)
                else -> ExprSubqueryRow(rel, constructor)
            }
        }

        override fun visitSubqueryComp(rex: RexSubqueryComp, ctx: Unit): Operator.Expr {
            TODO("<exists predicate> and <unique predicate>")
        }

        override fun visitSubqueryIn(rex: RexSubqueryIn, ctx: Unit): Operator.Expr {
            TODO("<in predicate>")
        }

        override fun visitSubqueryTest(rex: RexSubqueryTest, ctx: Unit): Operator.Expr {
            TODO("<exists predicate> and <unique predicate>")
        }

        override fun visitSpread(rex: RexSpread, ctx: Unit): Operator.Expr {
            val args = rex.getArgs().map { compile(it, ctx) }.toTypedArray()
            return ExprTupleUnion(args)
        }

        override fun visitTable(rex: RexTable, ctx: Unit): Operator.Expr {
            return ExprTable(rex.getTable())
        }

        override fun visitVar(rex: RexVar, ctx: Unit): Operator.Expr {
            val depth = rex.getDepth()
            val offset = rex.getOffset()
            return ExprVar(depth, offset)
        }
    }

    /**
     * Some places "catch" an error and return the MISSING value.
     */
    private fun Operator.Expr.catch(): Operator.Expr = when (mode) {
        PartiQLEngine.Mode.PERMISSIVE -> ExprPermissive(this)
        PartiQLEngine.Mode.STRICT -> this
    }
}
