@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.`internal`.ir

import org.partiql.planner.internal.ir.builder.CatalogBuilder
import org.partiql.planner.internal.ir.builder.CatalogItemFnBuilder
import org.partiql.planner.internal.ir.builder.CatalogItemValueBuilder
import org.partiql.planner.internal.ir.builder.FnResolvedBuilder
import org.partiql.planner.internal.ir.builder.FnUnresolvedBuilder
import org.partiql.planner.internal.ir.builder.IdentifierQualifiedBuilder
import org.partiql.planner.internal.ir.builder.IdentifierSymbolBuilder
import org.partiql.planner.internal.ir.builder.PartiQlPlanBuilder
import org.partiql.planner.internal.ir.builder.RefBuilder
import org.partiql.planner.internal.ir.builder.RelBindingBuilder
import org.partiql.planner.internal.ir.builder.RelBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateBuilder
import org.partiql.planner.internal.ir.builder.RelOpAggregateCallBuilder
import org.partiql.planner.internal.ir.builder.RelOpDistinctBuilder
import org.partiql.planner.internal.ir.builder.RelOpErrBuilder
import org.partiql.planner.internal.ir.builder.RelOpExceptBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeItemBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepCollIndexBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepCollWildcardBuilder
import org.partiql.planner.internal.ir.builder.RelOpExcludeStepStructFieldBuilder
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
import org.partiql.planner.internal.ir.builder.RexOpPathIndexBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathKeyBuilder
import org.partiql.planner.internal.ir.builder.RexOpPathSymbolBuilder
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
    @JvmField internal val catalogs: List<Catalog>,
    @JvmField internal val statement: Statement,
) : PlanNode() {
    override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.addAll(catalogs)
        kids.add(statement)
        kids.filterNotNull()
    }

    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitPartiQLPlan(this, ctx)

    internal companion object {
        @JvmStatic
        internal fun builder(): PartiQlPlanBuilder = PartiQlPlanBuilder()
    }
}

internal data class Catalog(
    @JvmField internal val name: String,
    @JvmField internal val items: List<Item>,
) : PlanNode() {
    override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.addAll(items)
        kids.filterNotNull()
    }

    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitCatalog(this, ctx)

    internal sealed class Item : PlanNode() {
        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Value -> visitor.visitCatalogItemValue(this, ctx)
            is Fn -> visitor.visitCatalogItemFn(this, ctx)
        }

        internal data class Value(
            @JvmField internal val path: List<String>,
            @JvmField internal val type: StaticType,
        ) : Item() {
            override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitCatalogItemValue(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): CatalogItemValueBuilder = CatalogItemValueBuilder()
            }
        }

        internal data class Fn(
            @JvmField internal val path: List<String>,
            @JvmField internal val specific: String,
        ) : Item() {
            override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitCatalogItemFn(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): CatalogItemFnBuilder = CatalogItemFnBuilder()
            }
        }
    }

    internal companion object {
        @JvmStatic
        internal fun builder(): CatalogBuilder = CatalogBuilder()
    }
}

internal data class Ref(
    @JvmField internal val catalog: Int,
    @JvmField internal val symbol: Int,
) : PlanNode() {
    override val children: List<PlanNode> = emptyList()

    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRef(
        this, ctx
    )

    internal companion object {
        @JvmStatic
        internal fun builder(): RefBuilder = RefBuilder()
    }
}

internal sealed class Fn : PlanNode() {
    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Resolved -> visitor.visitFnResolved(this, ctx)
        is Unresolved -> visitor.visitFnUnresolved(this, ctx)
    }

    internal data class Resolved(
        @JvmField internal val ref: Ref,
    ) : Fn() {
        override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(ref)
            kids.filterNotNull()
        }

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitFnResolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): FnResolvedBuilder = FnResolvedBuilder()
        }
    }

    internal data class Unresolved(
        @JvmField internal val identifier: Identifier,
    ) : Fn() {
        override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(identifier)
            kids.filterNotNull()
        }

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitFnUnresolved(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): FnUnresolvedBuilder = FnUnresolvedBuilder()
        }
    }
}

internal sealed class Statement : PlanNode() {
    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitStatementQuery(this, ctx)
    }

    internal data class Query(
        @JvmField internal val root: Rex,
    ) : Statement() {
        override val children: List<PlanNode> by lazy {
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

internal sealed class Identifier : PlanNode() {
    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Symbol -> visitor.visitIdentifierSymbol(this, ctx)
        is Qualified -> visitor.visitIdentifierQualified(this, ctx)
    }

    internal enum class CaseSensitivity {
        SENSITIVE, INSENSITIVE,
    }

    internal data class Symbol(
        @JvmField internal val symbol: String,
        @JvmField internal val caseSensitivity: CaseSensitivity,
    ) : Identifier() {
        override val children: List<PlanNode> = emptyList()

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitIdentifierSymbol(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): IdentifierSymbolBuilder = IdentifierSymbolBuilder()
        }
    }

    internal data class Qualified(
        @JvmField internal val root: Symbol,
        @JvmField internal val steps: List<Symbol>,
    ) : Identifier() {
        override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.addAll(steps)
            kids.filterNotNull()
        }

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitIdentifierQualified(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): IdentifierQualifiedBuilder = IdentifierQualifiedBuilder()
        }
    }
}

internal data class Rex(
    @JvmField internal val type: StaticType,
    @JvmField internal val op: Op,
) : PlanNode() {
    override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(op)
        kids.filterNotNull()
    }

    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRex(
        this, ctx
    )

    internal sealed class Op : PlanNode() {
        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
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
            @JvmField internal val `value`: PartiQLValue,
        ) : Op() {
            override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpLit(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpLitBuilder = RexOpLitBuilder()
            }
        }

        internal sealed class Var : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Resolved -> visitor.visitRexOpVarResolved(this, ctx)
                is Unresolved -> visitor.visitRexOpVarUnresolved(this, ctx)
            }

            internal enum class Scope {
                DEFAULT, LOCAL,
            }

            internal data class Resolved(
                @JvmField internal val ref: Int,
            ) : Var() {
                override val children: List<PlanNode> = emptyList()

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarResolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarResolvedBuilder = RexOpVarResolvedBuilder()
                }
            }

            internal data class Unresolved(
                @JvmField internal val identifier: Identifier,
                @JvmField internal val scope: Scope,
            ) : Var() {
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(identifier)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpVarUnresolved(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RexOpVarUnresolvedBuilder = RexOpVarUnresolvedBuilder()
                }
            }
        }

        internal data class Global(
            @JvmField internal val ref: Ref,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(ref)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpGlobal(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RexOpGlobalBuilder = RexOpGlobalBuilder()
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
                override val children: List<PlanNode> by lazy {
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
                override val children: List<PlanNode> by lazy {
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
                override val children: List<PlanNode> by lazy {
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

        internal sealed class Call : Op() {
            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Static -> visitor.visitRexOpCallStatic(this, ctx)
                is Dynamic -> visitor.visitRexOpCallDynamic(this, ctx)
            }

            internal data class Static(
                @JvmField internal val fn: Fn,
                @JvmField internal val args: List<Rex>,
            ) : Call() {
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(fn)
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
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.addAll(args)
                    kids.addAll(candidates)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexOpCallDynamic(this, ctx)

                internal data class Candidate(
                    @JvmField internal val fn: Fn,
                    @JvmField internal val coercions: List<Fn?>,
                ) : PlanNode() {
                    override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(fn)
                        kids.addAll(coercions)
                        kids.filterNotNull()
                    }

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            override val children: List<PlanNode> by lazy {
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
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(condition)
                    kids.add(rex)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            @JvmField internal val values: List<Rex>,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(fields)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRexOpStruct(this, ctx)

            internal data class Field(
                @JvmField internal val k: Rex,
                @JvmField internal val v: Rex,
            ) : PlanNode() {
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(k)
                    kids.add(v)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            override val children: List<PlanNode> by lazy {
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
            @JvmField internal val select: Select,
            @JvmField internal val coercion: Coercion,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(select)
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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

        internal data class Err(
            @JvmField internal val message: String,
        ) : Op() {
            override val children: List<PlanNode> = emptyList()

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
    override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(type)
        kids.add(op)
        kids.filterNotNull()
    }

    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRel(
        this, ctx
    )

    internal enum class Prop {
        ORDERED,
    }

    internal data class Type(
        @JvmField internal val schema: List<Binding>,
        @JvmField internal val props: Set<Prop>,
    ) : PlanNode() {
        override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(schema)
            kids.filterNotNull()
        }

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelType(this, ctx)

        internal companion object {
            @JvmStatic
            internal fun builder(): RelTypeBuilder = RelTypeBuilder()
        }
    }

    internal sealed class Op : PlanNode() {
        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(rex)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
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
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
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
            @JvmField internal val lhs: Rel,
            @JvmField internal val rhs: Rel,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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
            override val children: List<PlanNode> by lazy {
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

            internal data class Call(
                @JvmField internal val agg: Fn,
                @JvmField internal val args: List<Rex>,
            ) : PlanNode() {
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(agg)
                    kids.addAll(args)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
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
            @JvmField internal val input: Rel,
            @JvmField internal val items: List<Item>,
        ) : Op() {
            override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(input)
                kids.addAll(items)
                kids.filterNotNull()
            }

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpExclude(this, ctx)

            internal data class Item(
                @JvmField internal val root: Rex.Op.Var,
                @JvmField internal val steps: List<Step>,
            ) : PlanNode() {
                override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(root)
                    kids.addAll(steps)
                    kids.filterNotNull()
                }

                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRelOpExcludeItem(this, ctx)

                internal companion object {
                    @JvmStatic
                    internal fun builder(): RelOpExcludeItemBuilder = RelOpExcludeItemBuilder()
                }
            }

            internal sealed class Step : PlanNode() {
                override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                    is StructField -> visitor.visitRelOpExcludeStepStructField(this, ctx)
                    is CollIndex -> visitor.visitRelOpExcludeStepCollIndex(this, ctx)
                    is StructWildcard -> visitor.visitRelOpExcludeStepStructWildcard(this, ctx)
                    is CollWildcard -> visitor.visitRelOpExcludeStepCollWildcard(this, ctx)
                }

                internal data class StructField(
                    @JvmField internal val symbol: Identifier.Symbol,
                ) : Step() {
                    override val children: List<PlanNode> by lazy {
                        val kids = mutableListOf<PlanNode?>()
                        kids.add(symbol)
                        kids.filterNotNull()
                    }

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepStructField(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepStructFieldBuilder =
                            RelOpExcludeStepStructFieldBuilder()
                    }
                }

                internal data class CollIndex(
                    @JvmField internal val index: Int,
                ) : Step() {
                    override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepCollIndex(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepCollIndexBuilder = RelOpExcludeStepCollIndexBuilder()
                    }
                }

                internal data class StructWildcard(
                    @JvmField internal val ` `: Char = ' ',
                ) : Step() {
                    override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepStructWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepStructWildcardBuilder =
                            RelOpExcludeStepStructWildcardBuilder()
                    }
                }

                internal data class CollWildcard(
                    @JvmField internal val ` `: Char = ' ',
                ) : Step() {
                    override val children: List<PlanNode> = emptyList()

                    override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                        visitor.visitRelOpExcludeStepCollWildcard(this, ctx)

                    internal companion object {
                        @JvmStatic
                        internal fun builder(): RelOpExcludeStepCollWildcardBuilder =
                            RelOpExcludeStepCollWildcardBuilder()
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
            override val children: List<PlanNode> = emptyList()

            override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelOpErr(this, ctx)

            internal companion object {
                @JvmStatic
                internal fun builder(): RelOpErrBuilder = RelOpErrBuilder()
            }
        }
    }

    internal data class Binding(
        @JvmField internal val name: String,
        @JvmField internal val type: StaticType,
    ) : PlanNode() {
        override val children: List<PlanNode> = emptyList()

        override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitRelBinding(this, ctx)

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
