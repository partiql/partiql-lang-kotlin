package org.partiql.isl

import com.amazon.ionelement.api.TimestampElement
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import org.partiql.isl.visitor.IonSchemaVisitor

public abstract class IonSchemaNode {
  public open val children: List<IonSchemaNode> = emptyList()

  public abstract fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C? = null): R?
}

public data class Schema(
  public val version: Version,
  public val header: Header,
  public val definitions: List<Type>,
  public val footer: Footer
) : IonSchemaNode() {
  public override val children: List<IonSchemaNode> by lazy {
    val kids = mutableListOf<IonSchemaNode>()
    kids.add(header)
    kids.addAll(definitions)
    kids.add(footer)
    kids
  }


  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public data class Header(
  public val imports: List<Import>,
  public val userReservedFields: UserReservedFields
) : IonSchemaNode() {
  public override val children: List<IonSchemaNode> by lazy {
    val kids = mutableListOf<IonSchemaNode>()
    kids.addAll(imports)
    kids.add(userReservedFields)
    kids
  }


  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public data class UserReservedFields(
  public val header: List<String>,
  public val type: List<String>,
  public val footer: List<String>
) : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public class Footer : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public sealed class Import : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
    is Schema -> visitor.visit(this, ctx)
    is Type -> visitor.visit(this, ctx)
    is TypeAlias -> visitor.visit(this, ctx)
  }

  public data class Schema(
    public val id: String
  ) : Import() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Type(
    public val id: String,
    public val type: org.partiql.isl.Type
  ) : Import() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class TypeAlias(
    public val id: String,
    public val type: org.partiql.isl.Type,
    public val alias: String
  ) : Import() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }
}

public data class Type(
  public val name: String,
  public val constraints: List<Constraint>
) : IonSchemaNode() {
  public override val children: List<IonSchemaNode> by lazy {
    val kids = mutableListOf<IonSchemaNode>()
    kids.addAll(constraints)
    kids
  }


  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public sealed class Ref : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
    is Type -> visitor.visit(this, ctx)
    is Inline -> visitor.visit(this, ctx)
    is Import -> visitor.visit(this, ctx)
  }

  public data class Type(
    public val name: String,
    public val nullable: Boolean
  ) : Ref() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Inline(
    public val constraints: List<Constraint>,
    public val nullable: Boolean
  ) : Ref() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(constraints)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Import(
    public val schema: String,
    public val type: String,
    public val nullable: Boolean
  ) : Ref() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }
}

public sealed class Constraint : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
    is Range -> visitor.visit(this, ctx)
    is AllOf -> visitor.visit(this, ctx)
    is AnyOf -> visitor.visit(this, ctx)
    is Annotations -> visitor.visit(this, ctx)
    is Length -> visitor.visit(this, ctx)
    is Contains -> visitor.visit(this, ctx)
    is Element -> visitor.visit(this, ctx)
    is Exponent -> visitor.visit(this, ctx)
    is FieldNames -> visitor.visit(this, ctx)
    is Fields -> visitor.visit(this, ctx)
    is Ieee754Float -> visitor.visit(this, ctx)
    is Not -> visitor.visit(this, ctx)
    is OneOf -> visitor.visit(this, ctx)
    is OrderedElements -> visitor.visit(this, ctx)
    is Precision -> visitor.visit(this, ctx)
    is Regex -> visitor.visit(this, ctx)
    is TimestampOffset -> visitor.visit(this, ctx)
    is TimestampPrecision -> visitor.visit(this, ctx)
    is Type -> visitor.visit(this, ctx)
    is ValidValues -> visitor.visit(this, ctx)
  }

  public data class Range(
    public val range: org.partiql.isl.Range
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(range)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class AllOf(
    public val types: List<Ref>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(types)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class AnyOf(
    public val types: List<Ref>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(types)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public class Annotations : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public sealed class Length : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
      is Equals -> visitor.visit(this, ctx)
      is Range -> visitor.visit(this, ctx)
    }

    public data class Equals(
      public val measure: Measure,
      public val length: Int
    ) : Length() {
      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }

    public data class Range(
      public val measure: Measure,
      public val range: org.partiql.isl.Range.Int
    ) : Length() {
      public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode>()
        kids.add(range)
        kids
      }


      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }
  }

  public data class Contains(
    public val values: List<Value.Ion>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(values)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Element(
    public val type: Ref,
    public val distinct: Boolean
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public sealed class Exponent : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
      is Equals -> visitor.visit(this, ctx)
      is Range -> visitor.visit(this, ctx)
    }

    public data class Equals(
      public val `value`: Int
    ) : Exponent() {
      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }

    public data class Range(
      public val range: org.partiql.isl.Range.Int
    ) : Exponent() {
      public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode>()
        kids.add(range)
        kids
      }


      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }
  }

  public data class FieldNames(
    public val type: Ref,
    public val distinct: Boolean
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Fields(
    public val closed: Boolean,
    public val fields: List<Field>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(fields)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Ieee754Float(
    public val format: Format
  ) : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)

    public enum class Format {
      BINARY_16,
      BINARY_32,
      BINARY_64,
    }
  }

  public data class Not(
    public val type: Ref
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class OneOf(
    public val types: List<Ref>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(types)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class OrderedElements(
    public val types: List<Ref>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(types)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public sealed class Precision : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
      is Equals -> visitor.visit(this, ctx)
      is Range -> visitor.visit(this, ctx)
    }

    public data class Equals(
      public val `value`: Int
    ) : Precision() {
      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }

    public data class Range(
      public val range: org.partiql.isl.Range.Int
    ) : Precision() {
      public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode>()
        kids.add(range)
        kids
      }


      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }
  }

  public data class Regex(
    public val pattern: String,
    public val flags: Flags
  ) : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)

    public enum class Flags {
      MULTILINE,
      CASE_INSENSITIVE,
    }
  }

  public data class TimestampOffset(
    public val pattern: String
  ) : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public sealed class TimestampPrecision : Constraint() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
      is Equals -> visitor.visit(this, ctx)
      is Range -> visitor.visit(this, ctx)
    }

    public data class Equals(
      public val `value`: org.partiql.isl.TimestampPrecision
    ) : TimestampPrecision() {
      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }

    public data class Range(
      public val range: org.partiql.isl.Range.TimestampPrecision
    ) : TimestampPrecision() {
      public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode>()
        kids.add(range)
        kids
      }


      public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
          visitor.visit(this, ctx)
    }
  }

  public data class Type(
    public val type: Ref
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(type)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class ValidValues(
    public val values: List<Value>
  ) : Constraint() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.addAll(values)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }
}

public data class Field(
  public val name: String,
  public val type: Ref
) : IonSchemaNode() {
  public override val children: List<IonSchemaNode> by lazy {
    val kids = mutableListOf<IonSchemaNode>()
    kids.add(type)
    kids
  }


  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
      visitor.visit(this, ctx)
}

public sealed class Value : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
    is Ion -> visitor.visit(this, ctx)
    is Range -> visitor.visit(this, ctx)
  }

  public data class Ion(
    public val `value`: Ion
  ) : Value() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(value)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Range(
    public val `value`: Constraint.Range
  ) : Value() {
    public override val children: List<IonSchemaNode> by lazy {
      val kids = mutableListOf<IonSchemaNode>()
      kids.add(value)
      kids
    }


    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }
}

public sealed class Range : IonSchemaNode() {
  public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
    is Int -> visitor.visit(this, ctx)
    is Number -> visitor.visit(this, ctx)
    is Timestamp -> visitor.visit(this, ctx)
    is TimestampPrecision -> visitor.visit(this, ctx)
  }

  public data class Int(
    public val lower: kotlin.Int,
    public val upper: kotlin.Int,
    public val bounds: Bounds
  ) : Range() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Number(
    public val lower: Double,
    public val upper: Double,
    public val bounds: Bounds
  ) : Range() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class Timestamp(
    public val lower: TimestampElement,
    public val upper: TimestampElement,
    public val bounds: Bounds
  ) : Range() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }

  public data class TimestampPrecision(
    public val lower: org.partiql.isl.TimestampPrecision,
    public val upper: org.partiql.isl.TimestampPrecision,
    public val bounds: Bounds
  ) : Range() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
  }
}

public enum class Version {
  V1_0,
  V2_0,
}

public enum class Measure {
  BYTES,
  CODEPOINTS,
  ELEMENTS,
  UTF8,
}

public enum class TimestampPrecision {
  YEAR,
  MONTH,
  DAY,
  MIN,
  SECOND,
  MILLISECOND,
  MICROSECOND,
  NANOSECOND,
}

public enum class Bounds {
  INCLUSIVE,
  EXCLUSIVE,
  L_EXCLUSIVE,
  R_EXCLUSIVE,
}
