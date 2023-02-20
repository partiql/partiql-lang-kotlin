package org.partiql.plan.ir

import com.amazon.ionelement.api.IonElement
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.partiql.plan.ir.visitor.PlanVisitor

public abstract class PlanNode {
    public open val children: List<PlanNode> = emptyList()

    public abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
}

@JsonIgnoreProperties("children")
@JsonPropertyOrder("_id")
public data class Plan(
    public val version: Version,
    public val root: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(root)
        kids.filterNotNull()
    }

    @JsonProperty("_id")
    private val _id: String = "plan"

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitPlan(
        this,
        ctx
    )

    public enum class Version {
        PARTIQL_V0,
    }
}

@JsonIgnoreProperties("children")
@JsonPropertyOrder("_id")
public data class Common(
    public val schema: Map<String, Rel.Join.Type>,
    public val properties: Set<Property>,
    public val metas: Map<String, Any>
) : PlanNode() {
    @JsonProperty("_id")
    private val _id: String = "common"

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitCommon(this, ctx)
}

@JsonIgnoreProperties("children")
@JsonPropertyOrder("_id")
public data class Binding(
    public val name: Rex,
    public val rex: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(name)
        kids.add(rex)
        kids.filterNotNull()
    }

    @JsonProperty("_id")
    private val _id: String = "binding"

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitBinding(this, ctx)
}

public sealed class Step : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Rex -> visitor.visitStepRex(this, ctx)
        is Wildcard -> visitor.visitStepWildcard(this, ctx)
        is Unpivot -> visitor.visitStepUnpivot(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Rex(
        public val index: org.partiql.plan.ir.Rex,
        public val case: Case?
    ) : Step() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(index)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "step.rex"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepRex(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public class Wildcard : Step() {
        @JsonProperty("_id")
        private val _id: String = "step.wildcard"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepWildcard(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public class Unpivot : Step() {
        @JsonProperty("_id")
        private val _id: String = "step.unpivot"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStepUnpivot(this, ctx)
    }
}

@JsonIgnoreProperties("children")
@JsonPropertyOrder("_id")
public data class SortSpec(
    public val rex: Rex,
    public val dir: Dir,
    public val nulls: Nulls
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(rex)
        kids.filterNotNull()
    }

    @JsonProperty("_id")
    private val _id: String = "sort_spec"

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

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Scan(
        public val common: Common,
        public val rex: Rex,
        public val alias: String?,
        public val at: String?,
        public val `by`: String?
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(rex)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rel.scan"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelScan(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Unpivot(
        public val common: Common,
        public val rex: Rex,
        public val alias: String?,
        public val at: String?,
        public val `by`: String?
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(rex)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rel.unpivot"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelUnpivot(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.filter"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFilter(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.sort"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelSort(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.bag"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelBag(this, ctx)

        public enum class Op {
            UNION,
            INTERSECT,
            EXCEPT,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.fetch"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFetch(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.project"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelProject(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.join"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelJoin(this, ctx)

        public enum class Type {
            INNER,
            LEFT,
            RIGHT,
            FULL,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
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

        @JsonProperty("_id")
        private val _id: String = "rel.aggregate"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelAggregate(this, ctx)

        public enum class Strategy {
            FULL,
            PARTIAL,
        }
    }
}

public sealed class Rex : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Id -> visitor.visitRexId(this, ctx)
        is Path -> visitor.visitRexPath(this, ctx)
        is Unary -> visitor.visitRexUnary(this, ctx)
        is Binary -> visitor.visitRexBinary(this, ctx)
        is Call -> visitor.visitRexCall(this, ctx)
        is Agg -> visitor.visitRexAgg(this, ctx)
        is Lit -> visitor.visitRexLit(this, ctx)
        is Collection -> visitor.visitRexCollection(this, ctx)
        is Struct -> visitor.visitRexStruct(this, ctx)
        is Query -> visitor.visitRexQuery(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Id(
        public val name: String,
        public val case: Case?,
        public val qualifier: Qualifier
    ) : Rex() {
        @JsonProperty("_id")
        private val _id: String = "rex.id"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexId(this, ctx)

        public enum class Qualifier {
            UNQUALIFIED,
            LOCALS_FIRST,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Path(
        public val root: Rex,
        public val steps: List<Step>
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
            kids.addAll(steps)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.path"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexPath(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Unary(
        public val rex: Rex,
        public val op: Op
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(rex)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.unary"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexUnary(this, ctx)

        public enum class Op {
            NOT,
            POS,
            NEG,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Binary(
        public val lhs: Rex,
        public val rhs: Rex,
        public val op: Op
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.binary"

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
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Call(
        public val id: String,
        public val args: List<Rex>
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.call"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexCall(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Agg(
        public val id: String,
        public val args: List<Rex>,
        public val modifier: Modifier
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.agg"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexAgg(this, ctx)

        public enum class Modifier {
            ALL,
            DISTINCT,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Lit(
        public val `value`: IonElement
    ) : Rex() {
        @JsonProperty("_id")
        private val _id: String = "rex.lit"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexLit(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Collection(
        public val type: Type,
        public val values: List<Rex>
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(values)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.collection"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexCollection(this, ctx)

        public enum class Type {
            LIST,
            BAG,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Struct(
        public val fields: List<Binding>
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.addAll(fields)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "rex.struct"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexStruct(this, ctx)
    }

    public sealed class Query : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Scalar -> visitor.visitRexQueryScalar(this, ctx)
            is Collection -> visitor.visitRexQueryCollection(this, ctx)
        }

        public sealed class Scalar : Query() {
            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
                is Coerce -> visitor.visitRexQueryScalarCoerce(this, ctx)
                is Pivot -> visitor.visitRexQueryScalarPivot(this, ctx)
            }

            @JsonIgnoreProperties("children")
            @JsonPropertyOrder("_id")
            public data class Coerce(
                public val query: Collection
            ) : Scalar() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(query)
                    kids.filterNotNull()
                }

                @JsonProperty("_id")
                private val _id: String = "rex.query.scalar.coerce"

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexQueryScalarCoerce(this, ctx)
            }

            @JsonIgnoreProperties("children")
            @JsonPropertyOrder("_id")
            public data class Pivot(
                public val rel: Rel,
                public val rex: Rex,
                public val at: Rex
            ) : Scalar() {
                public override val children: List<PlanNode> by lazy {
                    val kids = mutableListOf<PlanNode?>()
                    kids.add(rel)
                    kids.add(rex)
                    kids.add(at)
                    kids.filterNotNull()
                }

                @JsonProperty("_id")
                private val _id: String = "rex.query.scalar.pivot"

                public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                    visitor.visitRexQueryScalarPivot(this, ctx)
            }
        }

        @JsonIgnoreProperties("children")
        @JsonPropertyOrder("_id")
        public data class Collection(
            public val rel: Rel,
            public val `constructor`: Rex?
        ) : Query() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rel)
                kids.add(constructor)
                kids.filterNotNull()
            }

            @JsonProperty("_id")
            private val _id: String = "rex.query.collection"

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexQueryCollection(this, ctx)
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
