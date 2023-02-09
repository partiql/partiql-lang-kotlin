package org.partiql.ir.rel

import org.partiql.ir.rel.visitor.PlanVisitor
import org.partiql.lang.domains.PartiqlAst.Expr

public abstract class PlanNode {
    public open val children: List<PlanNode> = emptyList()

    public abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
}

public data class Common(
    public val schema: Map<String, Rel.Join.Type>,
    public val properties: Set<Property>,
    public val metas: Map<String, Any>
) : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitCommon(this, ctx)
}

public sealed class Rel : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
        is Scan -> visitor.visitRelScan(this, ctx)
        is Cross -> visitor.visitRelCross(this, ctx)
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
        public val `value`: Expr,
        public val `as`: String?,
        public val at: String?,
        public val `by`: String?
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelScan(this, ctx)
    }

    public data class Cross(
        public val common: Common,
        public val lhs: Rel,
        public val rhs: Rel
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelCross(this, ctx)
    }

    public data class Filter(
        public val common: Common,
        public val input: Rel,
        public val condition: Expr
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFilter(this, ctx)
    }

    public data class Sort(
        public val common: Common,
        public val expr: Expr,
        public val dir: Dir,
        public val nulls: Nulls
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelSort(this, ctx)

        public enum class Dir {
            ASC,
            DESC,
        }

        public enum class Nulls {
            FIRST,
            LAST,
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
    }

    public data class Fetch(
        public val common: Common,
        public val input: Rel,
        public val limit: Long,
        public val offset: Long
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelFetch(this, ctx)
    }

    public data class Project(
        public val common: Common,
        public val input: Rel,
        public val exprs: List<Binding>
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.addAll(exprs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelProject(this, ctx)
    }

    public data class Join(
        public val common: Common,
        public val lhs: Rel,
        public val rhs: Rel,
        public val condition: Expr?,
        public val type: Type
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(lhs)
            kids.add(rhs)
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
    }

    public data class Aggregate(
        public val common: Common,
        public val input: Rel,
        public val calls: List<Binding>,
        public val groups: List<Expr>
    ) : Rel() {
        public override val children: List<PlanNode> by lazy {
            val kids = mutableListOf<PlanNode?>()
            kids.add(common)
            kids.add(input)
            kids.addAll(calls)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRelAggregate(this, ctx)
    }
}

public data class Binding(
    public val name: String,
    public val `value`: Expr
) : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitBinding(this, ctx)
}

public enum class Property {
    ORDERED,
}
