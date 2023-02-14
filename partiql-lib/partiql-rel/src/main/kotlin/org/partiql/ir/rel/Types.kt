package org.partiql.ir.rel

import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import org.partiql.ir.rel.visitor.RelVisitor
import org.partiql.ir.rex.RexNode

public abstract class RelNode {
  public open val children: List<RelNode> = emptyList()

  public abstract fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R
}

public data class Common(
  public val schema: Map<String, Rel.Join.Type>,
  public val properties: Set<Property>,
  public val metas: Map<String, Any>
) : RelNode() {
  public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
      visitor.visitCommon(this, ctx)
}

public sealed class Rel : RelNode() {
  public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = when (this) {
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
    public val `value`: RexNode,
    public val alias: String?,
    public val at: String?,
    public val `by`: String?
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelScan(this, ctx)
  }

  public data class Cross(
    public val common: Common,
    public val lhs: Rel,
    public val rhs: Rel
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(lhs)
      kids.add(rhs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelCross(this, ctx)
  }

  public data class Filter(
    public val common: Common,
    public val input: Rel,
    public val condition: RexNode
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(input)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelFilter(this, ctx)
  }

  public data class Sort(
    public val common: Common,
    public val input: Rel,
    public val specs: List<SortSpec>
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(input)
      kids.addAll(specs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelSort(this, ctx)
  }

  public data class Bag(
    public val common: Common,
    public val lhs: Rel,
    public val rhs: Rel,
    public val op: Op
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(lhs)
      kids.add(rhs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
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
    public val limit: RexNode,
    public val offset: RexNode
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(input)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelFetch(this, ctx)
  }

  public data class Project(
    public val common: Common,
    public val input: Rel,
    public val rexs: List<Binding>
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(input)
      kids.addAll(rexs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelProject(this, ctx)
  }

  public data class Join(
    public val common: Common,
    public val lhs: Rel,
    public val rhs: Rel,
    public val condition: RexNode?,
    public val type: Type
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(lhs)
      kids.add(rhs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
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
    public val groups: List<Binding>,
    public val strategy: Strategy
  ) : Rel() {
    public override val children: List<RelNode> by lazy {
      val kids = mutableListOf<RelNode?>()
      kids.add(common)
      kids.add(input)
      kids.addAll(calls)
      kids.addAll(groups)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitRelAggregate(this, ctx)

    public enum class Strategy {
      FULL,
      PARTIAL,
    }
  }
}

public data class SortSpec(
  public val rex: RexNode,
  public val dir: Dir,
  public val nulls: Nulls
) : RelNode() {
  public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
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

public data class Binding(
  public val name: String,
  public val rex: RexNode
) : RelNode() {
  public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
      visitor.visitBinding(this, ctx)
}

public enum class Property {
  ORDERED,
}
