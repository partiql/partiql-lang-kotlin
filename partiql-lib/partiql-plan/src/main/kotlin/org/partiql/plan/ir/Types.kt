package org.partiql.plan.ir

import com.amazon.ionelement.api.IonElement
import com.fasterxml.jackson.`annotation`.JsonIgnoreProperties
import com.fasterxml.jackson.`annotation`.JsonProperty
import com.fasterxml.jackson.`annotation`.JsonPropertyOrder
import org.partiql.plan.ir.visitor.PlanVisitor
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set

public abstract class PlanNode {
    public open val children: List<PlanNode> = emptyList()

    public abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
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

public sealed class Rel : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Scan -> visitor.visitRelScan(this, ctx)
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
        is Subquery -> visitor.visitRexSubquery(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Id(
        public val name: String
    ) : Rex() {
        @JsonProperty("_id")
        private val _id: String = "rex.id"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexId(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Path(
        public val root: Rex
    ) : Rex() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(root)
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
            SEXP,
        }
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Struct(
        public val fields: List<StructPart>
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

    public sealed class Subquery : Rex() {
        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
            is Tuple -> visitor.visitRexSubqueryTuple(this, ctx)
            is Scalar -> visitor.visitRexSubqueryScalar(this, ctx)
            is Collection -> visitor.visitRexSubqueryCollection(this, ctx)
        }

        @JsonIgnoreProperties("children")
        @JsonPropertyOrder("_id")
        public data class Tuple(
            public val rel: Rel
        ) : Subquery() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rel)
                kids.filterNotNull()
            }

            @JsonProperty("_id")
            private val _id: String = "rex.subquery.tuple"

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexSubqueryTuple(this, ctx)
        }

        @JsonIgnoreProperties("children")
        @JsonPropertyOrder("_id")
        public data class Scalar(
            public val rel: Rel
        ) : Subquery() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rel)
                kids.filterNotNull()
            }

            @JsonProperty("_id")
            private val _id: String = "rex.subquery.scalar"

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexSubqueryScalar(this, ctx)
        }

        @JsonIgnoreProperties("children")
        @JsonPropertyOrder("_id")
        public data class Collection(
            public val rel: Rel
        ) : Subquery() {
            public override val children: List<PlanNode> by lazy {
                val kids = mutableListOf<PlanNode?>()
                kids.add(rel)
                kids.filterNotNull()
            }

            @JsonProperty("_id")
            private val _id: String = "rex.subquery.collection"

            public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
                visitor.visitRexSubqueryCollection(this, ctx)
        }
    }
}

public sealed class StructPart : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Fields -> visitor.visitStructPartFields(this, ctx)
        is Field -> visitor.visitStructPartField(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Fields(
        public val rex: Rex
    ) : StructPart() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(rex)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "struct_part.fields"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStructPartFields(this, ctx)
    }

    @JsonIgnoreProperties("children")
    @JsonPropertyOrder("_id")
    public data class Field(
        public val name: Rex,
        public val rex: Rex
    ) : StructPart() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(name)
            kids.add(rex)
            kids.filterNotNull()
        }

        @JsonProperty("_id")
        private val _id: String = "struct_part.field"

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitStructPartField(this, ctx)
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

@JsonIgnoreProperties("children")
@JsonPropertyOrder("_id")
public data class Binding(
    public val name: String,
    public val rex: Rex
) : PlanNode() {
    public override val children: List<PlanNode> by lazy {
        val kids = mutableListOf<PlanNode?>()
        kids.add(rex)
        kids.filterNotNull()
    }

    @JsonProperty("_id")
    private val _id: String = "binding"

    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitBinding(this, ctx)
}

public enum class Property {
    ORDERED,
}
