package org.partiql.ir.rex

import com.amazon.ionelement.api.AnyElement
import kotlin.String
import kotlin.collections.List
import org.partiql.ir.rex.visitor.RexVisitor

public abstract class RexNode {
  public open val children: List<RexNode> = emptyList()

  public abstract fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R
}

public sealed class Rex : RexNode() {
  public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = when (this) {
    is Id -> visitor.visitRexId(this, ctx)
    is Path -> visitor.visitRexPath(this, ctx)
    is Unary -> visitor.visitRexUnary(this, ctx)
    is Binary -> visitor.visitRexBinary(this, ctx)
    is Call -> visitor.visitRexCall(this, ctx)
    is Agg -> visitor.visitRexAgg(this, ctx)
    is Lit -> visitor.visitRexLit(this, ctx)
  }

  public data class Id(
    public val name: String
  ) : Rex() {
    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexId(this, ctx)
  }

  public data class Path(
    public val root: Rex
  ) : Rex() {
    public override val children: List<RexNode> by lazy {
      val kids = mutableListOf<RexNode?>()
      kids.add(root)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexPath(this, ctx)
  }

  public data class Unary(
    public val rex: Rex,
    public val op: Op
  ) : Rex() {
    public override val children: List<RexNode> by lazy {
      val kids = mutableListOf<RexNode?>()
      kids.add(rex)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexUnary(this, ctx)

    public enum class Op {
      NOT,
      POS,
      NEG,
    }
  }

  public data class Binary(
    public val lhs: Rex,
    public val rhs: Rex,
    public val op: Op
  ) : Rex() {
    public override val children: List<RexNode> by lazy {
      val kids = mutableListOf<RexNode?>()
      kids.add(lhs)
      kids.add(rhs)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexBinary(this, ctx)

    public enum class Op {
      PLUS,
      MINUS,
      TIMES,
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

  public data class Call(
    public val id: String,
    public val args: List<Rex>
  ) : Rex() {
    public override val children: List<RexNode> by lazy {
      val kids = mutableListOf<RexNode?>()
      kids.addAll(args)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexCall(this, ctx)
  }

  public data class Agg(
    public val id: String,
    public val args: List<Rex>,
    public val modifier: Modifier?
  ) : Rex() {
    public override val children: List<RexNode> by lazy {
      val kids = mutableListOf<RexNode?>()
      kids.addAll(args)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
        visitor.visitRexAgg(this, ctx)

    public enum class Modifier {
      DISTINCT,
    }
  }

  public sealed class Lit : Rex() {
    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = when (this) {
      is Collection -> visitor.visitRexLitCollection(this, ctx)
      is Scalar -> visitor.visitRexLitScalar(this, ctx)
    }

    public data class Collection(
      public val type: Type,
      public val values: List<Rex>
    ) : Lit() {
      public override val children: List<RexNode> by lazy {
        val kids = mutableListOf<RexNode?>()
        kids.addAll(values)
        kids.filterNotNull()
      }


      public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
          visitor.visitRexLitCollection(this, ctx)

      public enum class Type {
        LIST,
        BAG,
        SEXP,
      }
    }

    public data class Scalar(
      public val `value`: AnyElement
    ) : Lit() {
      public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R =
          visitor.visitRexLitScalar(this, ctx)
    }
  }
}
