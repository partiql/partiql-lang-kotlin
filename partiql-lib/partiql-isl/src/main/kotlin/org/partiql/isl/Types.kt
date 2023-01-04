package org.partiql.isl

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.TimestampElement
import org.partiql.isl.visitor.IonSchemaVisitor

public abstract class IonSchemaNode {
    public open val children: List<IonSchemaNode> = emptyList()

    public abstract fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C? = null): R?
}

public data class Schema(
    public val version: Version,
    public val header: Header?,
    public val definitions: List<Definition>,
    public val footer: Footer?
) : IonSchemaNode() {
    public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode?>()
        kids.add(header)
        kids.addAll(definitions)
        kids.add(footer)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
}

public data class Header(
    public val imports: List<Import>,
    public val userReservedFields: UserReservedFields
) : IonSchemaNode() {
    public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode?>()
        kids.addAll(imports)
        kids.add(userReservedFields)
        kids.filterNotNull()
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
        public val type: String
    ) : Import() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class TypeAlias(
        public val id: String,
        public val type: String,
        public val alias: String
    ) : Import() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }
}

public data class Definition(
    public val name: String,
    public val constraints: List<Constraint>
) : IonSchemaNode() {
    public override val children: List<IonSchemaNode> by lazy {
        val kids = mutableListOf<IonSchemaNode?>()
        kids.addAll(constraints)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
        visitor.visit(this, ctx)
}

public sealed class Type : IonSchemaNode() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
        is Ref -> visitor.visit(this, ctx)
        is Inline -> visitor.visit(this, ctx)
        is Import -> visitor.visit(this, ctx)
    }

    public data class Ref(
        public val name: String,
        public val nullable: Boolean
    ) : Type() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Inline(
        public val constraints: List<Constraint>,
        public val nullable: Boolean,
        public val occurs: Occurs?
    ) : Type() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(constraints)
            kids.add(occurs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Import(
        public val schema: String,
        public val type: String,
        public val nullable: Boolean
    ) : Type() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }
}

public sealed class Constraint : IonSchemaNode() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
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

    public data class AllOf(
        public val types: List<org.partiql.isl.Type>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(types)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class AnyOf(
        public val types: List<org.partiql.isl.Type>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(types)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public sealed class Annotations : Constraint() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
            is Values -> visitor.visit(this, ctx)
            is Type -> visitor.visit(this, ctx)
        }

        public data class Values(
            public val modifier: Modifier,
            public val values: List<String>
        ) : Annotations() {
            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)

            public enum class Modifier {
                REQUIRED,
                CLOSED,
            }
        }

        public data class Type(
            public val type: org.partiql.isl.Type
        ) : Annotations() {
            public override val children: List<IonSchemaNode> by lazy {
                val kids = mutableListOf<IonSchemaNode?>()
                kids.add(type)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }
    }

    public sealed class Length : Constraint() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
            is Equals -> visitor.visit(this, ctx)
            is Range -> visitor.visit(this, ctx)
        }

        public data class Equals(
            public val measure: Measure,
            public val length: Long
        ) : Length() {
            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }

        public data class Range(
            public val measure: Measure,
            public val range: org.partiql.isl.Range.Int
        ) : Length() {
            public override val children: List<IonSchemaNode> by lazy {
                val kids = mutableListOf<IonSchemaNode?>()
                kids.add(range)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }
    }

    public data class Contains(
        public val values: List<Value.Ion>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(values)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Element(
        public val type: org.partiql.isl.Type,
        public val distinct: Boolean?
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(type)
            kids.filterNotNull()
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
            public val `value`: Long
        ) : Exponent() {
            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }

        public data class Range(
            public val range: org.partiql.isl.Range.Int
        ) : Exponent() {
            public override val children: List<IonSchemaNode> by lazy {
                val kids = mutableListOf<IonSchemaNode?>()
                kids.add(range)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }
    }

    public data class FieldNames(
        public val type: Type,
        public val distinct: Boolean?
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(type)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Fields(
        public val closed: Boolean?,
        public val fields: Map<String, org.partiql.isl.Type>
    ) : Constraint() {
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
        public val type: Type
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(type)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class OneOf(
        public val types: List<org.partiql.isl.Type>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(types)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class OrderedElements(
        public val types: List<org.partiql.isl.Type>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(types)
            kids.filterNotNull()
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
            public val `value`: Long
        ) : Precision() {
            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }

        public data class Range(
            public val range: org.partiql.isl.Range.Int
        ) : Precision() {
            public override val children: List<IonSchemaNode> by lazy {
                val kids = mutableListOf<IonSchemaNode?>()
                kids.add(range)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }
    }

    public data class Regex(
        public val pattern: String,
        public val flags: List<RegexFlag>
    ) : Constraint() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class TimestampOffset(
        public val offsets: List<String>
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
                val kids = mutableListOf<IonSchemaNode?>()
                kids.add(range)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
                visitor.visit(this, ctx)
        }
    }

    public data class Type(
        public val type: org.partiql.isl.Type
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(type)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class ValidValues(
        public val values: List<Value>
    ) : Constraint() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.addAll(values)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }
}

public sealed class Value : IonSchemaNode() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
        is Ion -> visitor.visit(this, ctx)
        is Range -> visitor.visit(this, ctx)
    }

    public data class Ion(
        public val `value`: IonElement
    ) : Value() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Range(
        public val `value`: org.partiql.isl.Range
    ) : Value() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(value)
            kids.filterNotNull()
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
        public val lower: Long?,
        public val upper: Long?,
        public val bounds: Bounds
    ) : Range() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Number(
        public val lower: Double?,
        public val upper: Double?,
        public val bounds: Bounds
    ) : Range() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Timestamp(
        public val lower: TimestampElement?,
        public val upper: TimestampElement?,
        public val bounds: Bounds
    ) : Range() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class TimestampPrecision(
        public val lower: org.partiql.isl.TimestampPrecision?,
        public val upper: org.partiql.isl.TimestampPrecision?,
        public val bounds: Bounds
    ) : Range() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }
}

public sealed class Occurs : IonSchemaNode() {
    public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? = when (this) {
        is Equal -> visitor.visit(this, ctx)
        is Range -> visitor.visit(this, ctx)
        is Optional -> visitor.visit(this, ctx)
        is Required -> visitor.visit(this, ctx)
    }

    public data class Equal(
        public val `value`: Long
    ) : Occurs() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public data class Range(
        public val range: org.partiql.isl.Range.Int
    ) : Occurs() {
        public override val children: List<IonSchemaNode> by lazy {
            val kids = mutableListOf<IonSchemaNode?>()
            kids.add(range)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public class Optional : Occurs() {
        public override fun <R, C> accept(visitor: IonSchemaVisitor<R, C>, ctx: C?): R? =
            visitor.visit(this, ctx)
    }

    public class Required : Occurs() {
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

public enum class RegexFlag {
    MULTILINE,
    CASE_INSENSITIVE,
}
