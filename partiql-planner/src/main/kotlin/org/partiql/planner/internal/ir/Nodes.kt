@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir

import org.partiql.planner.internal.ir.builder.AggResolvedBuilder
import org.partiql.planner.internal.ir.builder.AggUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.FnResolvedBuilder
import org.partiql.planner.internal.ir.builder.FnUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.GlobalBuilder
import org.partiql.planner.internal.ir.builder.IdentifierQualifiedBuilder
import org.partiql.planner.internal.ir.builder.IdentifierSymbolBuilder
import org.partiql.planner.internal.ir.builder.PartiQlPlanBuilder
import org.partiql.planner.internal.ir.builder.RelBindingBuilder
import org.partiql.planner.internal.ir.builder.RelBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateCallBuilder
import org.partiql.planner.internal.ir.builder.RelOpDistinctBuilder
import org.partiql.planner.internal.ir.builder.RelOpErrBuilder
import org.partiql.planner.internal.ir.builder.RelOpExceptBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeItemBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepAttrBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepCollectionWildcardBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepPosBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepStructWildcardBuilder
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
import org.partiql.planner.internal.ir.builder.RexOpCaseBranchBuilder
import org.partiql.planner.internal.ir.builder.RexOpCaseBuilder
import org.partiql.planner.internal.ir.builder.RexOpCollectionBuilder
import org.partiql.planner.internal.ir.builder.RexOpErrBuilder
import org.partiql.planner.internal.ir.builder.RexOpGlobalBuilder
import org.partiql.planner.internal.ir.builder.RexOpLitBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathStepIndexBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathStepSymbolBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathStepUnpivotBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathStepWildcardBuilder
import org.partiql.planner.internal.ir.builder.RexOpPivotBuilder
import org.partiql.planner.internal.ir.builder.RexOpSelectBuilder
import org.partiql.planner.internal.ir.builder.RexOpStructBuilder
import org.partiql.planner.internal.ir.builder.RexOpStructFieldBuilder
import org.partiql.planner.internal.ir.builder.RexOpSubqueryBuilder
import org.partiql.planner.internal.ir.builder.RexOpTupleUnionBuilder
import org.partiql.planner.internal.ir.builder.RexOpVarResolvedBuilder
import org.partiql.planner.internal.ir.builder.RexOpVarUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.StatementQueryBuilder
import org.partiql.planner.internal.ir.visitor.PlanVisitor
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionSignature
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
    @JvmField
    internal val version: PartiQLVersion,
    @JvmField
    internal val globals: List<Global>,
    @JvmField
    internal val statement: Statement,
) : PlanNode() {
    internal override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.addAll(globals)
        kids.add(statement)
        kids.filterNotNull()
    }

    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitPartiQLPlan(this, ctx)

    internal companion object {
        @JvmStatic
        internal fun builder(): PartiQlPlanBuilder = PartiQlPlanBuilder()
    }
}

internal data class Global(
    @JvmField
    internal val path: Identifier.Qualified,
    @JvmField
    internal val type: StaticType,
) : PlanNode() {
    internal override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(path)
        kids.filterNotNull()
    }

    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitGlobal(this, ctx)

    internal companion object {
        @JvmStatic
        internal fun builder(): GlobalBuilder = GlobalBuilder()
    }
}

internal sealed class Fn : PlanNode() {
    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Resolved -> visitor.visitFnResolved(this, ctx)
        is Unresolved -> visitor.visitFnUnresolved(this, ctx)
    }

    internal data class Resolved(
        @JvmField
        internal val signature: FunctionSignature.Scalar,
    ) : Fn() {
        internal override val children: List<PlanNode> = emptyList()

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitFnResolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): FnResolvedBuilder = FnResolvedBuilder()
        }
    }

    internal data class Unresolved(
        @JvmField
        internal val identifier: Identifier,
        @JvmField
        internal val isHidden: Boolean,
    ) : Fn() {
        internal override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(identifier)
            kids.filterNotNull()
        }

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitFnUnresolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): FnUnresolvedBuilder = FnUnresolvedBuilder()
        }
    }
}

internal sealed class Agg : PlanNode() {
    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Resolved -> visitor.visitAggResolved(this, ctx)
        is Unresolved -> visitor.visitAggUnresolved(this, ctx)
    }

    internal data class Resolved(
        @JvmField
        internal val signature: FunctionSignature.Aggregation,
    ) : Agg() {
        internal override val children: List<PlanNode> = emptyList()

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitAggResolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): AggResolvedBuilder = AggResolvedBuilder()
        }
    }

    internal data class Unresolved(
        @JvmField
        internal val identifier: Identifier,
    ) : Agg() {
        internal override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(identifier)
            kids.filterNotNull()
        }

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitAggUnresolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): AggUnresolvedBuilder = AggUnresolvedBuilder()
        }
    }
}

internal sealed class Statement : PlanNode() {
    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitStatementQuery(this, ctx)
    }

    internal data class Query(
        @JvmField
        internal val root: Rex,
    ) : Statement() {
        internal override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.filterNotNull()
        }

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStatementQuery(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): StatementQueryBuilder = StatementQueryBuilder()
        }
    }
}

internal sealed class Identifier : PlanNode() {
    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Symbol -> visitor.visitIdentifierSymbol(this, ctx)
        is Qualified -> visitor.visitIdentifierQualified(this, ctx)
    }

    internal enum class CaseSensitivity {
        SENSITIVE,
        INSENSITIVE,
    }

    internal data class Symbol(
        @JvmField
        internal val symbol: String,
        @JvmField
        internal val caseSensitivity: CaseSensitivity,
    ) : Identifier() {
        internal override val children: List<PlanNode> = emptyList()

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitIdentifierSymbol(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): IdentifierSymbolBuilder = IdentifierSymbolBuilder()
        }
    }

    internal data class Qualified(
        @JvmField
        internal val root: Symbol,
        @JvmField
        internal val steps: List<Symbol>,
    ) : Identifier() {
        internal override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.addAll(steps)
            kids.filterNotNull()
        }

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitIdentifierQualified(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): IdentifierQualifiedBuilder = IdentifierQualifiedBuilder()
        }
    }
}

internal data class Rex(
    @JvmField
    internal val type: StaticType,
    @JvmField
    internal val op: Op,
) : PlanNode() {
    internal override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(op)
        kids.filterNotNull()
    }

    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRex(
        this,
        ctx
    )

    internal sealed class Op : PlanNode() {
        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Lit -> visitor.visitRexOpLit(this, ctx)
            is Var -> visitor.visitRexOpVar(this, ctx)
            is Global -> visitor.visitRexOpGlobal(this, ctx)
            is Path -> visitor.visitRexOpPath(this, ctx)
            is Call -> visitor.visitRexOpCall(this, ctx)
            is Case -> visitor.visitRexOpCase(this, ctx)
            is Collection -> visitor.visitRexOpCollection(this, ctx)
            is Struct -> visitor.visitRexOpStruct(this, ctx)
            is Pivot -> visitor.visitRexOpPivot(this, ctx)
            is Subquery -> visitor.visitRexOpSubquery(this, ctx)
            is Select -> visitor.visitRexOpSelect(this, ctx)
            is TupleUnion -> visitor.visitRexOpTupleUnion(this, ctx)
            is Err -> visitor.visitRexOpErr(this, ctx)
        }

        internal data class Lit(
            @JvmField
            internal val `value`: PartiQLValue,
        ) : Op() {
            internal override val children: List<PlanNode> = emptyList()

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpLit(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpLitBuilder = RexOpLitBuilder()
            }
        }

        internal sealed class Var : Op() {
            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Resolved -> visitor.visitRexOpVarResolved(this, ctx)
                is Unresolved -> visitor.visitRexOpVarUnresolved(this, ctx)
            }

            internal enum class Scope {
                DEFAULT,
                LOCAL,
            }

            internal data class Resolved(
                @JvmField
                internal val ref: Int,
            ) : Var() {
                internal override val children: List<PlanNode> = emptyList()

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarResolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarResolvedBuilder = RexOpVarResolvedBuilder()
                }
            }

            internal data class Unresolved(
                @JvmField
                internal val identifier: Identifier,
                @JvmField
                internal val scope: Scope,
            ) : Var() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(identifier)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarUnresolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarUnresolvedBuilder = RexOpVarUnresolvedBuilder()
                }
            }
        }

        internal data class Global(
            @JvmField
            internal val ref: Int,
        ) : Op() {
            internal override val children: List<PlanNode> = emptyList()

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpGlobal(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpGlobalBuilder = RexOpGlobalBuilder()
            }
        }

        internal data class Path(
            @JvmField
            internal val root: Rex,
            @JvmField
            internal val steps: List<Step>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(root)
                kids.addAll(steps)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpPath(this, ctx)

            internal sealed class Step : PlanNode() {
                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                    is Index -> visitor.visitRexOpPathStepIndex(this, ctx)
                    is Symbol -> visitor.visitRexOpPathStepSymbol(this, ctx)
                    is Wildcard -> visitor.visitRexOpPathStepWildcard(this, ctx)
                    is Unpivot -> visitor.visitRexOpPathStepUnpivot(this, ctx)
                }

                internal data class Index(
                    @JvmField
                    internal val key: Rex,
                ) : Step() {
                    internal override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(key)
                        kids.filterNotNull()
                    }

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpPathStepIndex(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpPathStepIndexBuilder = RexOpPathStepIndexBuilder()
                    }
                }

                internal data class Symbol(
                    @JvmField
                    internal val identifier: Identifier.Symbol,
                ) : Step() {
                    internal override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(identifier)
                        kids.filterNotNull()
                    }

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpPathStepSymbol(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpPathStepSymbolBuilder = RexOpPathStepSymbolBuilder()
                    }
                }

                internal data class Wildcard(
                    @JvmField
                    internal val ` `: Char = ' ',
                ) : Step() {
                    internal override val children: List<PlanNode> = emptyList()

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpPathStepWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpPathStepWildcardBuilder = RexOpPathStepWildcardBuilder()
                    }
                }

                internal data class Unpivot(
                    @JvmField
                    internal val ` `: Char = ' ',
                ) : Step() {
                    internal override val children: List<PlanNode> = emptyList()

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpPathStepUnpivot(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpPathStepUnpivotBuilder = RexOpPathStepUnpivotBuilder()
                    }
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpPathBuilder = RexOpPathBuilder()
            }
        }

        internal sealed class Call : Op() {
            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Static -> visitor.visitRexOpCallStatic(this, ctx)
                is Dynamic -> visitor.visitRexOpCallDynamic(this, ctx)
            }

            internal data class Static(
                @JvmField
                internal val fn: Fn,
                @JvmField
                internal val args: List<Rex>,
            ) : Call() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(fn)
                    kids.addAll(args)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallStatic(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCallStaticBuilder = RexOpCallStaticBuilder()
                }
            }

            internal data class Dynamic(
                @JvmField
                internal val args: List<Rex>,
                @JvmField
                internal val candidates: List<Candidate>,
            ) : Call() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.addAll(args)
                    kids.addAll(candidates)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallDynamic(this, ctx)

                internal data class Candidate(
                    @JvmField
                    internal val fn: Fn.Resolved,
                    @JvmField
                    internal val coercions: List<Fn.Resolved?>,
                ) : PlanNode() {
                    internal override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(fn)
                        kids.addAll(coercions)
                        kids.filterNotNull()
                    }

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRexOpCallDynamicCandidate(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RexOpCallDynamicCandidateBuilder =
                            RexOpCallDynamicCandidateBuilder()
                    }
                }

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpCallDynamicBuilder = RexOpCallDynamicBuilder()
                }
            }
        }

        internal data class Case(
            @JvmField
            internal val branches: List<Branch>,
            @JvmField
            internal val default: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(branches)
                kids.add(default)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpCase(this, ctx)

            internal data class Branch(
                @JvmField
                internal val condition: Rex,
                @JvmField
                internal val rex: Rex,
            ) : PlanNode() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(condition)
                    kids.add(rex)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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

        internal data class Collection(
            @JvmField
            internal val values: List<Rex>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(values)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpCollection(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpCollectionBuilder = RexOpCollectionBuilder()
            }
        }

        internal data class Struct(
            @JvmField
            internal val fields: List<Field>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(fields)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpStruct(this, ctx)

            internal data class Field(
                @JvmField
                internal val k: Rex,
                @JvmField
                internal val v: Rex,
            ) : PlanNode() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(k)
                    kids.add(v)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            @JvmField
            internal val key: Rex,
            @JvmField
            internal val `value`: Rex,
            @JvmField
            internal val rel: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(key)
                kids.add(value)
                kids.add(rel)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpPivot(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpPivotBuilder = RexOpPivotBuilder()
            }
        }

        internal data class Subquery(
            @JvmField
            internal val select: Select,
            @JvmField
            internal val coercion: Coercion,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(select)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpSubquery(this, ctx)

            internal enum class Coercion {
                SCALAR,
                ROW,
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpSubqueryBuilder = RexOpSubqueryBuilder()
            }
        }

        internal data class Select(
            @JvmField
            internal val `constructor`: Rex,
            @JvmField
            internal val rel: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(constructor)
                kids.add(rel)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpSelect(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpSelectBuilder = RexOpSelectBuilder()
            }
        }

        internal data class TupleUnion(
            @JvmField
            internal val args: List<Rex>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(args)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpTupleUnion(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpTupleUnionBuilder = RexOpTupleUnionBuilder()
            }
        }

        internal data class Err(
            @JvmField
            internal val message: String,
        ) : Op() {
            internal override val children: List<PlanNode> = emptyList()

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexOpErr(this, ctx)

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
    @JvmField
    internal val type: Type,
    @JvmField
    internal val op: Op,
) : PlanNode() {
    internal override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(type)
        kids.add(op)
        kids.filterNotNull()
    }

    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRel(
        this,
        ctx
    )

    internal enum class Prop {
        ORDERED,
    }

    internal data class Type(
        @JvmField
        internal val schema: List<Binding>,
        @JvmField
        internal val props: Set<Prop>,
    ) : PlanNode() {
        internal override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(schema)
            kids.filterNotNull()
        }

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelType(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RelTypeBuilder = RelTypeBuilder()
        }
    }

    internal sealed class Op : PlanNode() {
        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
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
            @JvmField
            internal val rex: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpScan(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpScanBuilder = RelOpScanBuilder()
            }
        }

        internal data class ScanIndexed(
            @JvmField
            internal val rex: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpScanIndexed(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpScanIndexedBuilder = RelOpScanIndexedBuilder()
            }
        }

        internal data class Unpivot(
            @JvmField
            internal val rex: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rex)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpUnpivot(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpUnpivotBuilder = RelOpUnpivotBuilder()
            }
        }

        internal data class Distinct(
            @JvmField
            internal val input: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpDistinct(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpDistinctBuilder = RelOpDistinctBuilder()
            }
        }

        internal data class Filter(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val predicate: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(predicate)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpFilter(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpFilterBuilder = RelOpFilterBuilder()
            }
        }

        internal data class Sort(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val specs: List<Spec>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(specs)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpSort(this, ctx)

            internal enum class Order {
                ASC_NULLS_LAST,
                ASC_NULLS_FIRST,
                DESC_NULLS_LAST,
                DESC_NULLS_FIRST,
            }

            internal data class Spec(
                @JvmField
                internal val rex: Rex,
                @JvmField
                internal val order: Order,
            ) : PlanNode() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(rex)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            @JvmField
            internal val lhs: Rel,
            @JvmField
            internal val rhs: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpUnion(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpUnionBuilder = RelOpUnionBuilder()
            }
        }

        internal data class Intersect(
            @JvmField
            internal val lhs: Rel,
            @JvmField
            internal val rhs: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpIntersect(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpIntersectBuilder = RelOpIntersectBuilder()
            }
        }

        internal data class Except(
            @JvmField
            internal val lhs: Rel,
            @JvmField
            internal val rhs: Rel,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpExcept(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpExceptBuilder = RelOpExceptBuilder()
            }
        }

        internal data class Limit(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val limit: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(limit)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpLimit(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpLimitBuilder = RelOpLimitBuilder()
            }
        }

        internal data class Offset(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val offset: Rex,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.add(offset)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpOffset(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpOffsetBuilder = RelOpOffsetBuilder()
            }
        }

        internal data class Project(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val projections: List<Rex>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(projections)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpProject(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpProjectBuilder = RelOpProjectBuilder()
            }
        }

        internal data class Join(
            @JvmField
            internal val lhs: Rel,
            @JvmField
            internal val rhs: Rel,
            @JvmField
            internal val rex: Rex,
            @JvmField
            internal val type: Type,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(lhs)
                kids.add(rhs)
                kids.add(rex)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpJoin(this, ctx)

            internal enum class Type {
                INNER,
                LEFT,
                RIGHT,
                FULL,
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpJoinBuilder = RelOpJoinBuilder()
            }
        }

        internal data class Aggregate(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val strategy: Strategy,
            @JvmField
            internal val calls: List<Call>,
            @JvmField
            internal val groups: List<Rex>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(calls)
                kids.addAll(groups)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpAggregate(this, ctx)

            internal enum class Strategy {
                FULL,
                PARTIAL,
            }

            internal data class Call(
                @JvmField
                internal val agg: Agg,
                @JvmField
                internal val args: List<Rex>,
            ) : PlanNode() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(agg)
                    kids.addAll(args)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpAggregateCall(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpAggregateCallBuilder = RelOpAggregateCallBuilder()
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpAggregateBuilder = RelOpAggregateBuilder()
            }
        }

        internal data class Exclude(
            @JvmField
            internal val input: Rel,
            @JvmField
            internal val items: List<Item>,
        ) : Op() {
            internal override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(items)
                kids.filterNotNull()
            }

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpExclude(this, ctx)

            internal data class Item(
                @JvmField
                internal val root: Identifier.Symbol,
                @JvmField
                internal val steps: List<Step>,
            ) : PlanNode() {
                internal override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.addAll(steps)
                    kids.filterNotNull()
                }

                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpExcludeItem(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpExcludeItemBuilder = RelOpExcludeItemBuilder()
                }
            }

            internal sealed class Step : PlanNode() {
                internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                    is Attr -> visitor.visitRelOpExcludeStepAttr(this, ctx)
                    is Pos -> visitor.visitRelOpExcludeStepPos(this, ctx)
                    is StructWildcard -> visitor.visitRelOpExcludeStepStructWildcard(this, ctx)
                    is CollectionWildcard -> visitor.visitRelOpExcludeStepCollectionWildcard(this, ctx)
                }

                internal data class Attr(
                    @JvmField
                    internal val symbol: Identifier.Symbol,
                ) : Step() {
                    internal override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(symbol)
                        kids.filterNotNull()
                    }

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepAttr(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepAttrBuilder = RelOpExcludeStepAttrBuilder()
                    }
                }

                internal data class Pos(
                    @JvmField
                    internal val index: Int,
                ) : Step() {
                    internal override val children: List<PlanNode> = emptyList()

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepPos(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepPosBuilder = RelOpExcludeStepPosBuilder()
                    }
                }

                internal data class StructWildcard(
                    @JvmField
                    internal val ` `: Char = ' ',
                ) : Step() {
                    internal override val children: List<PlanNode> = emptyList()

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepStructWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepStructWildcardBuilder =
                            RelOpExcludeStepStructWildcardBuilder()
                    }
                }

                internal data class CollectionWildcard(
                    @JvmField
                    internal val ` `: Char = ' ',
                ) : Step() {
                    internal override val children: List<PlanNode> = emptyList()

                    internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepCollectionWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepCollectionWildcardBuilder =
                            RelOpExcludeStepCollectionWildcardBuilder()
                    }
                }
            }

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpExcludeBuilder = RelOpExcludeBuilder()
            }
        }

        internal data class Err(
            @JvmField
            internal val message: String,
        ) : Op() {
            internal override val children: List<PlanNode> = emptyList()

            internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRelOpErr(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpErrBuilder = RelOpErrBuilder()
            }
        }
    }

    internal data class Binding(
        @JvmField
        internal val name: String,
        @JvmField
        internal val type: StaticType,
    ) : PlanNode() {
        internal override val children: List<PlanNode> = emptyList()

        internal override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelBinding(this, ctx)

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

internal enum class PartiQLVersion {
    VERSION_0_0,
    VERSION_0_1,
}
