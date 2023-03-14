package org.partiql.plan

import com.amazon.ionelement.api.IonElement
import org.partiql.plan.builder.ArgTypeBuilder
import org.partiql.plan.builder.ArgValueBuilder
import org.partiql.plan.builder.AttributeBuilder
import org.partiql.plan.builder.BindingBuilder
import org.partiql.plan.builder.BranchBuilder
import org.partiql.plan.builder.CommonBuilder
import org.partiql.plan.builder.FieldBuilder
import org.partiql.plan.builder.PartiQlPlanBuilder
import org.partiql.plan.builder.RelAggregateBuilder
import org.partiql.plan.builder.RelBagBuilder
import org.partiql.plan.builder.RelFetchBuilder
import org.partiql.plan.builder.RelFilterBuilder
import org.partiql.plan.builder.RelJoinBuilder
import org.partiql.plan.builder.RelProjectBuilder
import org.partiql.plan.builder.RelScanBuilder
import org.partiql.plan.builder.RelSortBuilder
import org.partiql.plan.builder.RelUnpivotBuilder
import org.partiql.plan.builder.RexAggBuilder
import org.partiql.plan.builder.RexBinaryBuilder
import org.partiql.plan.builder.RexCallBuilder
import org.partiql.plan.builder.RexCollectionArrayBuilder
import org.partiql.plan.builder.RexCollectionBagBuilder
import org.partiql.plan.builder.RexIdBuilder
import org.partiql.plan.builder.RexLitBuilder
import org.partiql.plan.builder.RexPathBuilder
import org.partiql.plan.builder.RexQueryCollectionBuilder
import org.partiql.plan.builder.RexQueryScalarPivotBuilder
import org.partiql.plan.builder.RexQueryScalarSubqueryBuilder
import org.partiql.plan.builder.RexSwitchBuilder
import org.partiql.plan.builder.RexTupleBuilder
import org.partiql.plan.builder.RexUnaryBuilder
import org.partiql.plan.builder.SortSpecBuilder
import org.partiql.plan.builder.StepKeyBuilder
import org.partiql.plan.builder.StepUnpivotBuilder
import org.partiql.plan.builder.StepWildcardBuilder
import org.partiql.plan.visitor.PlanVisitor
import org.partiql.types.StaticType
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.JvmStatic

public abstract class PlanNode {
    public open val children: List<PlanNode> = emptyList()

    public abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
}

public data class PartiQLPlan(
    public val version: Version,
    public val root: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(root)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitPartiQLPlan(this, ctx)

    public enum class Version {
        PARTIQL_V0,
    }

    public companion object {
        @JvmStatic
        public fun builder(): PartiQlPlanBuilder = PartiQlPlanBuilder()
    }
}

public data class Common(
    public val schema: List<Attribute>,
    public val properties: Set<Property>,
    public val metas: Map<String, Any>
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.addAll(schema)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitCommon(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): CommonBuilder = CommonBuilder()
    }
}

public data class Attribute(
    public val name: String,
    public val type: StaticType
) : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitAttribute(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): AttributeBuilder = AttributeBuilder()
    }
}

public data class Binding(
    public val name: String,
    public val `value`: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(value)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitBinding(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): BindingBuilder = BindingBuilder()
    }
}

public data class Field(
    public val name: Rex,
    public val `value`: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(name)
        kids.add(value)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitField(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): FieldBuilder = FieldBuilder()
    }
}

public sealed class Step : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Key -> visitor.visitStepKey(this, ctx)
        is Wildcard -> visitor.visitStepWildcard(this, ctx)
        is Unpivot -> visitor.visitStepUnpivot(this, ctx)
    }

    public data class Key(
        public val `value`: Rex,
        public val case: Case
    ) : Step() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepKey(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): StepKeyBuilder = StepKeyBuilder()
        }
    }

    public class Wildcard : Step() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepWildcard(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): StepWildcardBuilder = StepWildcardBuilder()
        }
    }

    public class Unpivot : Step() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepUnpivot(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): StepUnpivotBuilder = StepUnpivotBuilder()
        }
    }
}

public data class SortSpec(
    public val `value`: Rex,
    public val dir: Dir,
    public val nulls: Nulls
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(value)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitSortSpec(this, ctx)

    public enum class Dir {
        ASC,
        DESC,
    }

    public enum class Nulls {
        FIRST,
        LAST,
    }

    public companion object {
        @JvmStatic
        public fun builder(): SortSpecBuilder = SortSpecBuilder()
    }
}

public sealed class Arg : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Value -> visitor.visitArgValue(this, ctx)
        is Type -> visitor.visitArgType(this, ctx)
    }

    public data class Value(
        public val name: String?,
        public val `value`: Rex
    ) : Arg() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitArgValue(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ArgValueBuilder = ArgValueBuilder()
        }
    }

    public data class Type(
        public val name: String?,
        public val type: StaticType
    ) : Arg() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitArgType(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ArgTypeBuilder = ArgTypeBuilder()
        }
    }
}

public data class Branch(
    public val condition: Rex,
    public val `value`: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(condition)
        kids.add(value)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitBranch(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): BranchBuilder = BranchBuilder()
    }
}

public sealed class Rel : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Scan -> visitor.visitRelScan(this, ctx)
        is Unpivot -> visitor.visitRelUnpivot(this, ctx)
        is Filter -> visitor.visitRelFilter(this, ctx)
        is Sort -> visitor.visitRelSort(this, ctx)
        is Bag -> visitor.visitRelBag(this, ctx)
        is Fetch -> visitor.visitRelFetch(this, ctx)
        is Project -> visitor.visitRelProject(this, ctx)
        is Join -> visitor.visitRelJoin(this, ctx)
        is Aggregate -> visitor.visitRelAggregate(this, ctx)
    }

    public data class Scan(
        public val common: Common,
        public val `value`: Rex,
        public val alias: String?,
        public val at: String?,
        public val `by`: String?
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelScan(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelScanBuilder = RelScanBuilder()
        }
    }

    public data class Unpivot(
        public val common: Common,
        public val `value`: Rex,
        public val alias: String?,
        public val at: String?,
        public val `by`: String?
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelUnpivot(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelUnpivotBuilder = RelUnpivotBuilder()
        }
    }

    public data class Filter(
        public val common: Common,
        public val input: Rel,
        public val condition: Rex
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.add(condition)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFilter(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelFilterBuilder = RelFilterBuilder()
        }
    }

    public data class Sort(
        public val common: Common,
        public val input: Rel,
        public val specs: List<SortSpec>
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.addAll(specs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelSort(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelSortBuilder = RelSortBuilder()
        }
    }

    public data class Bag(
        public val common: Common,
        public val lhs: Rel,
        public val rhs: Rel,
        public val op: Op
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelBag(this, ctx)

        public enum class Op {
            UNION,
            INTERSECT,
            EXCEPT,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RelBagBuilder = RelBagBuilder()
        }
    }

    public data class Fetch(
        public val common: Common,
        public val input: Rel,
        public val limit: Rex,
        public val offset: Rex
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.add(limit)
            kids.add(offset)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFetch(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelFetchBuilder = RelFetchBuilder()
        }
    }

    public data class Project(
        public val common: Common,
        public val input: Rel,
        public val bindings: List<Binding>
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.addAll(bindings)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelProject(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RelProjectBuilder = RelProjectBuilder()
        }
    }

    public data class Join(
        public val common: Common,
        public val lhs: Rel,
        public val rhs: Rel,
        public val condition: Rex?,
        public val type: Type
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(lhs)
            kids.add(rhs)
            kids.add(condition)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelJoin(this, ctx)

        public enum class Type {
            INNER,
            LEFT,
            RIGHT,
            FULL,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RelJoinBuilder = RelJoinBuilder()
        }
    }

    public data class Aggregate(
        public val common: Common,
        public val input: Rel,
        public val calls: List<Binding>,
        public val groups: List<Binding>,
        public val strategy: Strategy
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.addAll(calls)
            kids.addAll(groups)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelAggregate(this, ctx)

        public enum class Strategy {
            FULL,
            PARTIAL,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RelAggregateBuilder = RelAggregateBuilder()
        }
    }
}

public sealed class Rex : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Id -> visitor.visitRexId(this, ctx)
        is Path -> visitor.visitRexPath(this, ctx)
        is Lit -> visitor.visitRexLit(this, ctx)
        is Unary -> visitor.visitRexUnary(this, ctx)
        is Binary -> visitor.visitRexBinary(this, ctx)
        is Call -> visitor.visitRexCall(this, ctx)
        is Switch -> visitor.visitRexSwitch(this, ctx)
        is Agg -> visitor.visitRexAgg(this, ctx)
        is Collection -> visitor.visitRexCollection(this, ctx)
        is Tuple -> visitor.visitRexTuple(this, ctx)
        is Query -> visitor.visitRexQuery(this, ctx)
    }

    public data class Id(
        public val name: String,
        public val case: Case,
        public val qualifier: Qualifier,
        public val type: StaticType?
    ) : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexId(this, ctx)

        public enum class Qualifier {
            UNQUALIFIED,
            LOCALS_FIRST,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RexIdBuilder = RexIdBuilder()
        }
    }

    public data class Path(
        public val root: Rex,
        public val steps: List<Step>,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.addAll(steps)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexPath(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RexPathBuilder = RexPathBuilder()
        }
    }

    public data class Lit(
        public val `value`: IonElement,
        public val type: StaticType?
    ) : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexLit(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RexLitBuilder = RexLitBuilder()
        }
    }

    public data class Unary(
        public val `value`: Rex,
        public val op: Op,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexUnary(this, ctx)

        public enum class Op {
            NOT,
            POS,
            NEG,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RexUnaryBuilder = RexUnaryBuilder()
        }
    }

    public data class Binary(
        public val lhs: Rex,
        public val rhs: Rex,
        public val op: Op,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexBinary(this, ctx)

        public enum class Op {
            PLUS,
            MINUS,
            TIMES,
            DIV,
            MODULO,
            CONCAT,
            AND,
            OR,
            EQ,
            NEQ,
            GTE,
            GT,
            LT,
            LTE,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RexBinaryBuilder = RexBinaryBuilder()
        }
    }

    public data class Call(
        public val id: String,
        public val args: List<Arg>,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexCall(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RexCallBuilder = RexCallBuilder()
        }
    }

    public data class Switch(
        public val match: Rex?,
        public val branches: List<Branch>,
        public val default: Rex?,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(match)
            kids.addAll(branches)
            kids.add(default)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexSwitch(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RexSwitchBuilder = RexSwitchBuilder()
        }
    }

    public data class Agg(
        public val id: String,
        public val args: List<Rex>,
        public val modifier: Modifier,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexAgg(this, ctx)

        public enum class Modifier {
            ALL,
            DISTINCT,
        }

        public companion object {
            @JvmStatic
            public fun builder(): RexAggBuilder = RexAggBuilder()
        }
    }

    public sealed class Collection : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Array -> visitor.visitRexCollectionArray(this, ctx)
            is Bag -> visitor.visitRexCollectionBag(this, ctx)
        }

        public data class Array(
            public val values: List<Rex>,
            public val type: StaticType?
        ) : Collection() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(values)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexCollectionArray(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): RexCollectionArrayBuilder = RexCollectionArrayBuilder()
            }
        }

        public data class Bag(
            public val values: List<Rex>,
            public val type: StaticType?
        ) : Collection() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.addAll(values)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexCollectionBag(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): RexCollectionBagBuilder = RexCollectionBagBuilder()
            }
        }
    }

    public data class Tuple(
        public val fields: List<Field>,
        public val type: StaticType?
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(fields)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexTuple(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): RexTupleBuilder = RexTupleBuilder()
        }
    }

    public sealed class Query : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Scalar -> visitor.visitRexQueryScalar(this, ctx)
            is Collection -> visitor.visitRexQueryCollection(this, ctx)
        }

        public sealed class Scalar : Query() {
            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Subquery -> visitor.visitRexQueryScalarSubquery(this, ctx)
                is Pivot -> visitor.visitRexQueryScalarPivot(this, ctx)
            }

            public data class Subquery(
                public val query: Collection,
                public val type: StaticType?
            ) : Scalar() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(query)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexQueryScalarSubquery(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): RexQueryScalarSubqueryBuilder = RexQueryScalarSubqueryBuilder()
                }
            }

            public data class Pivot(
                public val rel: Rel,
                public val `value`: Rex,
                public val at: Rex,
                public val type: StaticType?
            ) : Scalar() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(rel)
                    kids.add(value)
                    kids.add(at)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexQueryScalarPivot(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): RexQueryScalarPivotBuilder = RexQueryScalarPivotBuilder()
                }
            }
        }

        public data class Collection(
            public val rel: Rel,
            public val `constructor`: Rex?,
            public val type: StaticType?
        ) : Query() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rel)
                kids.add(constructor)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexQueryCollection(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): RexQueryCollectionBuilder = RexQueryCollectionBuilder()
            }
        }
    }
}

public enum class Property {
    ORDERED,
}

public enum class Case {
    SENSITIVE,
    INSENSITIVE,
}
