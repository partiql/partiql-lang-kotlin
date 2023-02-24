package org.partiql.plan.ir

import com.amazon.ionelement.api.IonElement
<<<<<<< HEAD
=======
import org.partiql.lang.types.StaticType
import org.partiql.plan.ir.visitor.PlanVisitor
>>>>>>> 780d0657 (Adds SPI framework, schema inference, cli tool, localdb connector, and plan typing)
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import org.partiql.plan.ir.visitor.PlanVisitor

public abstract class PlanNode {
  public open val children: List<PlanNode> = emptyList()

  public abstract fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R
}

public data class Plan(
  public val version: Version,
  public val root: Rex
) : PlanNode() {
  public override val children: List<PlanNode> by lazy {
    val kids = mutableListOf<PlanNode?>()
    kids.add(root)
    kids.filterNotNull()
  }


  public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = visitor.visitPlan(this,
      ctx)

  public enum class Version {
    PARTIQL_V0,
  }
}

public data class Common(
<<<<<<< HEAD
  public val schema: Map<String, Rel.Join.Type>,
  public val properties: Set<Property>,
  public val metas: Map<String, Any>
) : PlanNode() {
  public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
      visitor.visitCommon(this, ctx)
=======
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
>>>>>>> 780d0657 (Adds SPI framework, schema inference, cli tool, localdb connector, and plan typing)
}

public data class Attribute(
    public val name: String,
    public val type: StaticType
) : PlanNode() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitAttribute(this, ctx)
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
}

public sealed class Step : PlanNode() {
  public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R = when (this) {
    is Key -> visitor.visitStepKey(this, ctx)
    is Wildcard -> visitor.visitStepWildcard(this, ctx)
    is Unpivot -> visitor.visitStepUnpivot(this, ctx)
  }

  public data class Key(
    public val `value`: Rex,
    public val case: Case?
  ) : Step() {
    public override val children: List<PlanNode> by lazy {
      val kids = mutableListOf<PlanNode?>()
      kids.add(value)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitStepKey(this, ctx)
  }

  public class Wildcard : Step() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitStepWildcard(this, ctx)
  }

  public class Unpivot : Step() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitStepUnpivot(this, ctx)
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
    is Tuple -> visitor.visitRexTuple(this, ctx)
    is Query -> visitor.visitRexQuery(this, ctx)
  }

  public data class Id(
    public val name: String,
    public val case: Case?,
    public val qualifier: Qualifier
  ) : Rex() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexId(this, ctx)

    public enum class Qualifier {
      UNQUALIFIED,
      LOCALS_FIRST,
    }
  }

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


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexPath(this, ctx)
  }

  public data class Unary(
    public val `value`: Rex,
    public val op: Op
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
  }

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

  public data class Call(
    public val id: String,
    public val args: List<Rex>
  ) : Rex() {
    public override val children: List<PlanNode> by lazy {
      val kids = mutableListOf<PlanNode?>()
      kids.addAll(args)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexCall(this, ctx)
  }

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


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexAgg(this, ctx)

    public enum class Modifier {
      ALL,
      DISTINCT,
    }
  }

  public data class Lit(
    public val `value`: IonElement
  ) : Rex() {
    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexLit(this, ctx)
  }

  public data class Collection(
    public val type: Type,
    public val values: List<Rex>
  ) : Rex() {
    public override val children: List<PlanNode> by lazy {
      val kids = mutableListOf<PlanNode?>()
      kids.addAll(values)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexCollection(this, ctx)

    public enum class Type {
      ARRAY,
      BAG,
    }
  }

  public data class Tuple(
    public val fields: List<Field>
  ) : Rex() {
    public override val children: List<PlanNode> by lazy {
      val kids = mutableListOf<PlanNode?>()
      kids.addAll(fields)
      kids.filterNotNull()
    }


    public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
        visitor.visitRexTuple(this, ctx)
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

      public data class Coerce(
        public val query: Collection
      ) : Scalar() {
        public override val children: List<PlanNode> by lazy {
          val kids = mutableListOf<PlanNode?>()
          kids.add(query)
          kids.filterNotNull()
        }


        public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
            visitor.visitRexQueryScalarCoerce(this, ctx)
      }

      public data class Pivot(
        public val rel: Rel,
        public val `value`: Rex,
        public val at: Rex
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
      }
    }

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
