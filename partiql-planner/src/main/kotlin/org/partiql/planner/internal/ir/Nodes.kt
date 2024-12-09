@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.`internal`.ir

import org.partiql.planner.internal.ir.builder.PartiQlPlanBuilder
import org.partiql.planner.internal.ir.builder.RefAggBuilder
import org.partiql.planner.internal.ir.builder.RefCastBuilder
import org.partiql.planner.internal.ir.builder.RefFnBuilder
import org.partiql.planner.internal.ir.builder.RefObjBuilder
import org.partiql.planner.internal.ir.builder.RelBindingBuilder
import org.partiql.planner.internal.ir.builder.RelBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateCallResolvedBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateCallUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.RelOpDistinctBuilder
import org.partiql.planner.internal.ir.builder.RelOpErrBuilder
import org.partiql.planner.internal.ir.builder.RelOpExceptBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludePathBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeTypeCollIndexBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeTypeCollWildcardBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeTypeStructKeyBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeTypeStructSymbolBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeTypeStructWildcardBuilder
import org.partiql.planner.internal.ir.builder.RelOpFilterBuilder
import org.partiql.planner.internal.ir.builder.RelOpIntersectBuilder
import org.partiql.planner.internal.ir.builder.RelOpJoinBuilder
import org.partiql.planner.internal.ir.builder.RelOpLimitBuilder
import org.partiql.planner.internal.ir.builder.RelOpOffsetBuilder
import org.partiql.planner.internal.ir.builder.RelOpProjectBuilder
import org.partiql.planner.internal.ir.builder.RelOpScanBuilder
import org.partiql.planner.internal.ir.builder.RelOpScanIndexedBuilder
import org.partiql.planner.internal.ir.builder.RelOpSortBuilder
import org.partiql.planner.internal.ir.builder.RelOpSortSpecBuilder
import org.partiql.planner.internal.ir.builder.RelOpUnionBuilder
import org.partiql.planner.internal.ir.builder.RelOpUnpivotBuilder
import org.partiql.planner.internal.ir.builder.RelTypeBuilder
import org.partiql.planner.internal.ir.builder.RexBuilder
import org.partiql.planner.internal.ir.builder.RexOpCallDynamicBuilder
import org.partiql.planner.internal.ir.builder.RexOpCallDynamicCandidateBuilder
import org.partiql.planner.internal.ir.builder.RexOpCallStaticBuilder
import org.partiql.planner.internal.ir.builder.RexOpCallUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.RexOpCaseBranchBuilder
import org.partiql.planner.internal.ir.builder.RexOpCaseBuilder
import org.partiql.planner.internal.ir.builder.RexOpCastResolvedBuilder
import org.partiql.planner.internal.ir.builder.RexOpCastUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.RexOpCoalesceBuilder
import org.partiql.planner.internal.ir.builder.RexOpCollectionBuilder
import org.partiql.planner.internal.ir.builder.RexOpErrBuilder
import org.partiql.planner.internal.ir.builder.RexOpLitBuilder
import org.partiql.planner.internal.ir.builder.RexOpNullifBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathIndexBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathKeyBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathSymbolBuilder
import org.partiql.planner.internal.ir.builder.RexOpPivotBuilder
import org.partiql.planner.internal.ir.builder.RexOpSelectBuilder
import org.partiql.planner.internal.ir.builder.RexOpStructBuilder
import org.partiql.planner.internal.ir.builder.RexOpStructFieldBuilder
import org.partiql.planner.internal.ir.builder.RexOpSubqueryBuilder
import org.partiql.planner.internal.ir.builder.RexOpTupleUnionBuilder
import org.partiql.planner.internal.ir.builder.RexOpVarGlobalBuilder
import org.partiql.planner.internal.ir.builder.RexOpVarLocalBuilder
import org.partiql.planner.internal.ir.builder.RexOpVarUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.StatementQueryBuilder
import org.partiql.planner.internal.ir.visitor.PlanVisitor
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Function
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import kotlin.random.Random

internal abstract class PlanNode {
    @JvmField
    internal var tag: String = "Plan-${"%06x".format(Random.nextInt())}"

    internal abstract val children: List<PlanNode>

    internal abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
}

internal data class PartiQLPlan(
    @JvmField internal val statement: Statement,
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(statement)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitPartiQLPlan(this, ctx)

    internal companion object {
        @JvmStatic
        internal fun builder(): PartiQlPlanBuilder = PartiQlPlanBuilder()
    }
}

internal sealed class Ref : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Obj -> visitor.visitRefObj(this, ctx)
        is Fn -> visitor.visitRefFn(this, ctx)
        is Agg -> visitor.visitRefAgg(this, ctx)
    }

    internal data class Obj(
        @JvmField internal val catalog: String,
        @JvmField internal val name: Name,
        @JvmField internal val type: CompilerType,
        @JvmField internal val table: Table,
    ) : Ref() {
        public override val children: List<PlanNode> = emptyList()

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRefObj(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RefObjBuilder = RefObjBuilder()
        }
    }

    internal data class Fn(
        @JvmField internal val catalog: String,
        @JvmField internal val name: Name,
        @JvmField internal val signature: Function,
    ) : Ref() {
        public override val children: List<PlanNode> = emptyList()

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRefFn(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RefFnBuilder = RefFnBuilder()
        }
    }

    internal data class Agg(
        @JvmField internal val catalog: String,
        @JvmField internal val name: Name,
        @JvmField internal val signature: Aggregation,
    ) : Ref() {
        public override val children: List<PlanNode> = emptyList()

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRefAgg(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RefAggBuilder = RefAggBuilder()
        }
    }

    internal data class Cast(
        @JvmField internal val input: CompilerType,
        @JvmField internal val target: CompilerType,
        @JvmField internal val safety: Safety,
        @JvmField internal val isNullable: Boolean,
    ) : PlanNode() {
        public override val children: List<PlanNode> = emptyList()

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRefCast(this, ctx)

        internal enum class Safety {
            COERCION, EXPLICIT, UNSAFE,
        }

        internal companion object {
            @JvmStatic
            internal fun builder(): RefCastBuilder = RefCastBuilder()
        }
    }
}

internal sealed class Statement : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitStatementQuery(this, ctx)
    }

    internal data class Query(
        @JvmField internal val root: Rex,
    ) : Statement() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.filterNotNull()
        }

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitStatementQuery(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): StatementQueryBuilder = StatementQueryBuilder()
        }
    }
}

internal data class Rex(
    @JvmField internal val type: CompilerType,
    @JvmField internal val op: Op,
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(op)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRex(
        this, ctx
    )

    internal sealed class Op : PlanNode() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Lit -> visitor.visitRexOpLit(this, ctx)
            is Var -> visitor.visitRexOpVar(this, ctx)
            is Path -> visitor.visitRexOpPath(this, ctx)
            is Cast -> visitor.visitRexOpCast(this, ctx)
            is Call -> visitor.visitRexOpCall(this, ctx)
            is Case -> visitor.visitRexOpCase(this, ctx)
            is Nullif -> visitor.visitRexOpNullif(this, ctx)
            is Coalesce -> visitor.visitRexOpCoalesce(this, ctx)
            is Collection -> visitor.visitRexOpCollection(this, ctx)
            is Struct -> visitor.visitRexOpStruct(this, ctx)
            is Pivot -> visitor.visitRexOpPivot(this, ctx)
            is Subquery -> visitor.visitRexOpSubquery(this, ctx)
            is Select -> visitor.visitRexOpSelect(this, ctx)
            is TupleUnion -> visitor.visitRexOpTupleUnion(this, ctx)
            is Err -> visitor.visitRexOpErr(this, ctx)
        }

        internal data class Lit(
            @JvmField internal val `value`: PartiQLValue,
        ) : Op() {
            public override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpLit(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpLitBuilder = RexOpLitBuilder()
            }
        }

        internal sealed class Var : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Local -> visitor.visitRexOpVarLocal(this, ctx)
                is Global -> visitor.visitRexOpVarGlobal(this, ctx)
                is Unresolved -> visitor.visitRexOpVarUnresolved(this, ctx)
            }

            internal enum class Scope {
                DEFAULT, LOCAL,
            }

            internal data class Local(
                @JvmField internal val depth: Int,
                @JvmField internal val ref: Int,
            ) : Var() {
                public override val children: List<PlanNode> = emptyList()

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarLocal(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarLocalBuilder = RexOpVarLocalBuilder()
                }
            }

            internal data class Global(
                @JvmField internal val ref: Ref.Obj,
            ) : Var() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(ref)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarGlobal(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarGlobalBuilder = RexOpVarGlobalBuilder()
                }
            }

            internal data class Unresolved(
                @JvmField internal val identifier: Identifier,
                @JvmField internal val scope: Scope,
            ) : Var() {
                public override val children: List<PlanNode> = emptyList()

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarUnresolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarUnresolvedBuilder = RexOpVarUnresolvedBuilder()
                }
            }
        }

        internal sealed class Path : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Index -> visitor.visitRexOpPathIndex(this, ctx)
                is Key -> visitor.visitRexOpPathKey(this, ctx)
                is Symbol -> visitor.visitRexOpPathSymbol(this, ctx)
            }

            internal data class Index(
                @JvmField internal val root: Rex,
                @JvmField internal val key: Rex,
            ) : Path() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.add(key)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpPathIndex(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpPathIndexBuilder = RexOpPathIndexBuilder()
                }
            }

            internal data class Key(
                @JvmField internal val root: Rex,
                @JvmField internal val key: Rex,
            ) : Path() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.add(key)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpPathKey(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpPathKeyBuilder = RexOpPathKeyBuilder()
                }
            }

            internal data class Symbol(
                @JvmField internal val root: Rex,
                @JvmField internal val key: String,
            ) : Path() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpPathSymbol(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpPathSymbolBuilder = RexOpPathSymbolBuilder()
                }
            }
        }

        internal sealed class Cast : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Unresolved -> visitor.visitRexOpCastUnresolved(this, ctx)
                is Resolved -> visitor.visitRexOpCastResolved(this, ctx)
            }

            internal data class Unresolved(
                @JvmField internal val target: CompilerType,
                @JvmField internal val arg: Rex,
            ) : Cast() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(arg)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCastUnresolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCastUnresolvedBuilder = RexOpCastUnresolvedBuilder()
                }
            }

            internal data class Resolved(
                @JvmField internal val cast: Ref.Cast,
                @JvmField internal val arg: Rex,
            ) : Cast() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(cast)
                    kids.add(arg)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCastResolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCastResolvedBuilder = RexOpCastResolvedBuilder()
                }
            }
        }

        internal sealed class Call : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Unresolved -> visitor.visitRexOpCallUnresolved(this, ctx)
                is Static -> visitor.visitRexOpCallStatic(this, ctx)
                is Dynamic -> visitor.visitRexOpCallDynamic(this, ctx)
            }

            internal data class Unresolved(
                @JvmField internal val identifier: Identifier,
                @JvmField internal val args: List<Rex>,
            ) : Call() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.addAll(args)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallUnresolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCallUnresolvedBuilder = RexOpCallUnresolvedBuilder()
                }
            }

            internal data class Static(
                @JvmField internal val fn: Function.Instance,
                @JvmField internal val args: List<Rex>,
            ) : Call() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.addAll(args)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallStatic(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCallStaticBuilder = RexOpCallStaticBuilder()
                }
            }

            internal data class Dynamic(
                @JvmField internal val args: List<Rex>,
                @JvmField internal val candidates: List<Candidate>,
            ) : Call() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.addAll(args)
                    kids.addAll(candidates)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallDynamic(this, ctx)

                internal data class Candidate(
                    @JvmField internal val fn: Ref.Fn,
                    @JvmField internal val coercions: List<Ref.Cast?>,
                ) : PlanNode() {
                    public override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(fn)
                        kids.addAll(coercions)
                        kids.filterNotNull()
                    }

                    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpCallDynamicCandidate(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpCallDynamicCandidateBuilder = RexOpCallDynamicCandidateBuilder()
                    }
                }

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCallDynamicBuilder = RexOpCallDynamicBuilder()
                }
            }
        }

        internal data class Case(
            @JvmField internal val branches: List<Branch>,
            @JvmField internal val default: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(branches)
                kids.add(default)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpCase(this, ctx)

            internal data class Branch(
                @JvmField internal val condition: Rex,
                @JvmField internal val rex: Rex,
            ) : PlanNode() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(condition)
                    kids.add(rex)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCaseBranch(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCaseBranchBuilder = RexOpCaseBranchBuilder()
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpCaseBuilder = RexOpCaseBuilder()
            }
        }

        internal data class Nullif(
            @JvmField internal val `value`: Rex,
            @JvmField internal val nullifier: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(value)
                kids.add(nullifier)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpNullif(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpNullifBuilder = RexOpNullifBuilder()
            }
        }

        internal data class Coalesce(
            @JvmField internal val args: List<Rex>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(args)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpCoalesce(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpCoalesceBuilder = RexOpCoalesceBuilder()
            }
        }

        internal data class Collection(
            @JvmField internal val values: List<Rex>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(values)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpCollection(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpCollectionBuilder = RexOpCollectionBuilder()
            }
        }

        internal data class Struct(
            @JvmField internal val fields: List<Field>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(fields)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpStruct(this, ctx)

            internal data class Field(
                @JvmField internal val k: Rex,
                @JvmField internal val v: Rex,
            ) : PlanNode() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(k)
                    kids.add(v)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpStructField(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpStructFieldBuilder = RexOpStructFieldBuilder()
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpStructBuilder = RexOpStructBuilder()
            }
        }

        internal data class Pivot(
            @JvmField internal val key: Rex,
            @JvmField internal val `value`: Rex,
            @JvmField internal val rel: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(key)
                kids.add(value)
                kids.add(rel)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpPivot(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpPivotBuilder = RexOpPivotBuilder()
            }
        }

        internal data class Subquery(
            @JvmField internal val `constructor`: Rex,
            @JvmField internal val rel: Rel,
            @JvmField internal val coercion: Coercion,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(constructor)
                kids.add(rel)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpSubquery(this, ctx)

            internal enum class Coercion {
                SCALAR, ROW,
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpSubqueryBuilder = RexOpSubqueryBuilder()
            }
        }

        internal data class Select(
            @JvmField internal val `constructor`: Rex,
            @JvmField internal val rel: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(constructor)
                kids.add(rel)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpSelect(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpSelectBuilder = RexOpSelectBuilder()
            }
        }

        internal data class TupleUnion(
            @JvmField internal val args: List<Rex>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(args)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpTupleUnion(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpTupleUnionBuilder = RexOpTupleUnionBuilder()
            }
        }

        internal class Err : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpErr(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpErrBuilder = RexOpErrBuilder()
            }
        }
    }

    internal companion object {
        @JvmStatic
        internal fun builder(): RexBuilder = RexBuilder()
    }
}

internal data class Rel(
    @JvmField internal val type: Type,
    @JvmField internal val op: Op,
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(type)
        kids.add(op)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRel(
        this, ctx
    )

    internal enum class Prop {
        ORDERED,
    }

    internal data class Type(
        @JvmField internal val schema: List<Binding>,
        @JvmField internal val props: Set<Prop>,
    ) : PlanNode() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(schema)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelType(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RelTypeBuilder = RelTypeBuilder()
        }
    }

    internal sealed class Op : PlanNode() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Scan -> visitor.visitRelOpScan(this, ctx)
            is ScanIndexed -> visitor.visitRelOpScanIndexed(this, ctx)
            is Unpivot -> visitor.visitRelOpUnpivot(this, ctx)
            is Distinct -> visitor.visitRelOpDistinct(this, ctx)
            is Filter -> visitor.visitRelOpFilter(this, ctx)
            is Sort -> visitor.visitRelOpSort(this, ctx)
            is Union -> visitor.visitRelOpUnion(this, ctx)
            is Intersect -> visitor.visitRelOpIntersect(this, ctx)
            is Except -> visitor.visitRelOpExcept(this, ctx)
            is Limit -> visitor.visitRelOpLimit(this, ctx)
            is Offset -> visitor.visitRelOpOffset(this, ctx)
            is Project -> visitor.visitRelOpProject(this, ctx)
            is Join -> visitor.visitRelOpJoin(this, ctx)
            is Aggregate -> visitor.visitRelOpAggregate(this, ctx)
            is Exclude -> visitor.visitRelOpExclude(this, ctx)
            is Err -> visitor.visitRelOpErr(this, ctx)
        }

        internal data class Scan(
            @JvmField internal val rex: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpScan(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpScanBuilder = RelOpScanBuilder()
            }
        }

        internal data class ScanIndexed(
            @JvmField internal val rex: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpScanIndexed(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpScanIndexedBuilder = RelOpScanIndexedBuilder()
            }
        }

        internal data class Unpivot(
            @JvmField internal val rex: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpUnpivot(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpUnpivotBuilder = RelOpUnpivotBuilder()
            }
        }

        internal data class Distinct(
            @JvmField internal val input: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpDistinct(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpDistinctBuilder = RelOpDistinctBuilder()
            }
        }

        internal data class Filter(
            @JvmField internal val input: Rel,
            @JvmField internal val predicate: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(predicate)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpFilter(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpFilterBuilder = RelOpFilterBuilder()
            }
        }

        internal data class Sort(
            @JvmField internal val input: Rel,
            @JvmField internal val specs: List<Spec>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(specs)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpSort(this, ctx)

            internal enum class Order {
                ASC_NULLS_LAST, ASC_NULLS_FIRST, DESC_NULLS_LAST, DESC_NULLS_FIRST,
            }

            internal data class Spec(
                @JvmField internal val rex: Rex,
                @JvmField internal val order: Order,
            ) : PlanNode() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(rex)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpSortSpec(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpSortSpecBuilder = RelOpSortSpecBuilder()
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpSortBuilder = RelOpSortBuilder()
            }
        }

        internal data class Union(
            @JvmField internal val setq: SetQuantifier,
            @JvmField internal val isOuter: Boolean,
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpUnion(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpUnionBuilder = RelOpUnionBuilder()
            }
        }

        internal data class Intersect(
            @JvmField internal val setq: SetQuantifier,
            @JvmField internal val isOuter: Boolean,
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpIntersect(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpIntersectBuilder = RelOpIntersectBuilder()
            }
        }

        internal data class Except(
            @JvmField internal val setq: SetQuantifier,
            @JvmField internal val isOuter: Boolean,
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpExcept(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpExceptBuilder = RelOpExceptBuilder()
            }
        }

        internal data class Limit(
            @JvmField internal val input: Rel,
            @JvmField internal val limit: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(limit)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpLimit(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpLimitBuilder = RelOpLimitBuilder()
            }
        }

        internal data class Offset(
            @JvmField internal val input: Rel,
            @JvmField internal val offset: Rex,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(offset)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpOffset(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpOffsetBuilder = RelOpOffsetBuilder()
            }
        }

        internal data class Project(
            @JvmField internal val input: Rel,
            @JvmField internal val projections: List<Rex>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(projections)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpProject(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpProjectBuilder = RelOpProjectBuilder()
            }
        }

        internal data class Join(
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
            @JvmField internal val rex: Rex,
            @JvmField internal val type: Type,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.add(rex)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpJoin(this, ctx)

            internal enum class Type {
                INNER, LEFT, RIGHT, FULL,
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpJoinBuilder = RelOpJoinBuilder()
            }
        }

        internal data class Aggregate(
            @JvmField internal val input: Rel,
            @JvmField internal val strategy: Strategy,
            @JvmField internal val calls: List<Call>,
            @JvmField internal val groups: List<Rex>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(calls)
                kids.addAll(groups)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpAggregate(this, ctx)

            internal enum class Strategy {
                FULL, PARTIAL,
            }

            internal sealed class Call : PlanNode() {
                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                    is Unresolved -> visitor.visitRelOpAggregateCallUnresolved(this, ctx)
                    is Resolved -> visitor.visitRelOpAggregateCallResolved(this, ctx)
                }

                internal data class Unresolved(
                    @JvmField internal val name: String,
                    @JvmField internal val setq: SetQuantifier,
                    @JvmField internal val args: List<Rex>,
                ) : Call() {
                    public override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.addAll(args)
                        kids.filterNotNull()
                    }

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpAggregateCallUnresolved(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpAggregateCallUnresolvedBuilder =
                            RelOpAggregateCallUnresolvedBuilder()
                    }
                }

                internal data class Resolved(
                    @JvmField internal val agg: Ref.Agg,
                    @JvmField internal val setq: SetQuantifier,
                    @JvmField internal val args: List<Rex>,
                ) : Call() {
                    public override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(agg)
                        kids.addAll(args)
                        kids.filterNotNull()
                    }

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpAggregateCallResolved(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpAggregateCallResolvedBuilder = RelOpAggregateCallResolvedBuilder()
                    }
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpAggregateBuilder = RelOpAggregateBuilder()
            }
        }

        internal data class Exclude(
            @JvmField internal val input: Rel,
            @JvmField internal val paths: List<Path>,
        ) : Op() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(paths)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpExclude(this, ctx)

            internal data class Path(
                @JvmField internal val root: Rex.Op,
                @JvmField internal val steps: List<Step>,
            ) : PlanNode() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.addAll(steps)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpExcludePath(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpExcludePathBuilder = RelOpExcludePathBuilder()
                }
            }

            internal data class Step(
                @JvmField internal val type: Type,
                @JvmField internal val substeps: List<Step>,
            ) : PlanNode() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(type)
                    kids.addAll(substeps)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpExcludeStep(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpExcludeStepBuilder = RelOpExcludeStepBuilder()
                }
            }

            internal sealed class Type : PlanNode() {
                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                    is StructSymbol -> visitor.visitRelOpExcludeTypeStructSymbol(this, ctx)
                    is StructKey -> visitor.visitRelOpExcludeTypeStructKey(this, ctx)
                    is CollIndex -> visitor.visitRelOpExcludeTypeCollIndex(this, ctx)
                    is StructWildcard -> visitor.visitRelOpExcludeTypeStructWildcard(this, ctx)
                    is CollWildcard -> visitor.visitRelOpExcludeTypeCollWildcard(this, ctx)
                }

                internal data class StructSymbol(
                    @JvmField internal val symbol: String,
                ) : Type() {
                    public override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeTypeStructSymbol(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeTypeStructSymbolBuilder =
                            RelOpExcludeTypeStructSymbolBuilder()
                    }

                    // Explicitly override `equals` and `hashcode` for case-insensitivity
                    override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (javaClass != other?.javaClass) return false

                        other as StructSymbol

                        if (!symbol.equals(other.symbol, ignoreCase = true)) return false
                        if (children != other.children) return false

                        return true
                    }

                    override fun hashCode(): Int {
                        return symbol.lowercase().hashCode()
                    }
                }

                internal data class StructKey(
                    @JvmField internal val key: String,
                ) : Type() {
                    public override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeTypeStructKey(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeTypeStructKeyBuilder = RelOpExcludeTypeStructKeyBuilder()
                    }
                }

                internal data class CollIndex(
                    @JvmField internal val index: Int,
                ) : Type() {
                    public override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeTypeCollIndex(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeTypeCollIndexBuilder = RelOpExcludeTypeCollIndexBuilder()
                    }
                }

                internal data class StructWildcard(
                    @JvmField internal val ` `: Char = ' ',
                ) : Type() {
                    public override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeTypeStructWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeTypeStructWildcardBuilder =
                            RelOpExcludeTypeStructWildcardBuilder()
                    }
                }

                internal data class CollWildcard(
                    @JvmField internal val ` `: Char = ' ',
                ) : Type() {
                    public override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeTypeCollWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeTypeCollWildcardBuilder =
                            RelOpExcludeTypeCollWildcardBuilder()
                    }
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpExcludeBuilder = RelOpExcludeBuilder()
            }
        }

        internal data class Err(
            @JvmField internal val message: String,
        ) : Op() {
            public override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpErr(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpErrBuilder = RelOpErrBuilder()
            }
        }
    }

    internal data class Binding(
        @JvmField internal val name: String,
        @JvmField internal val type: CompilerType,
    ) : PlanNode() {
        public override val children: List<PlanNode> = emptyList()

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelBinding(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RelBindingBuilder = RelBindingBuilder()
        }
    }

    internal companion object {
        @JvmStatic
        internal fun builder(): RelBuilder = RelBuilder()
    }
}

internal enum class SetQuantifier {
    ALL, DISTINCT,
}
