package org.partiql.planner.intern.validate

import org.partiql.planner.intern.SqlTypes
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.ListType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLTimestampExperimental
import org.partiql.value.PartiQLValueType

internal object StaticTypes : SqlTypes<StaticType> {

    override fun create(type: PartiQLValueType): StaticType = TODO("Not yet implemented")

    override fun create(type: PartiQLValueType, vararg args: Any): StaticType = TODO("Not yet implemented")

    override fun dynamic(): StaticType = StaticType.ANY

    /**
     * If ANY is one of the types in the variants, we return ANY. Else, return a UNION type.
     */
    override fun dynamic(variants: Set<StaticType>): StaticType {
        return if (variants.any { it is AnyType }) dynamic() else AnyOfType(variants).flatten()
    }

    override fun bool() = StaticType.BOOL

    override fun tinyint(): StaticType = error("StaticTypes does not support 'tinyint'")

    override fun smallint(): StaticType = StaticType.INT2

    override fun int(): StaticType = StaticType.INT4

    override fun bigint(): StaticType = StaticType.INT8

    override fun numeric(): StaticType = StaticType.INT

    override fun decimal(precision: Int): StaticType {
        return DecimalType(
            DecimalType.PrecisionScaleConstraint.Constrained(
                precision = precision,
                scale = 0,
            )
        )
    }

    override fun decimal(precision: Int, scale: Int): StaticType {
        return DecimalType(
            DecimalType.PrecisionScaleConstraint.Constrained(
                precision = precision,
                scale = scale,
            )
        )
    }

    override fun decimal(): StaticType {
        return DecimalType(
            DecimalType.PrecisionScaleConstraint.Unconstrained
        )
    }

    override fun float(precision: Int): StaticType = FloatType()

    override fun real(): StaticType = FloatType()

    override fun double(): StaticType = FloatType()

    override fun char(length: Int): StaticType {
        return StringType(
            StringType.StringLengthConstraint.Constrained(
                NumberConstraint.Equals(length)
            )
        )
    }

    override fun varchar(): StaticType {
        return StringType(
            StringType.StringLengthConstraint.Unconstrained
        )
    }

    override fun varchar(length: Int): StaticType {
        return StringType(
            StringType.StringLengthConstraint.Constrained(
                NumberConstraint.UpTo(length)
            )
        )
    }

    override fun date(): StaticType = StaticType.DATE

    override fun time(): StaticType = StaticType.TIME

    override fun time(precision: Int): StaticType = TimeType(precision)

    override fun timestamp(): StaticType = StaticType.TIMESTAMP

    @OptIn(PartiQLTimestampExperimental::class)
    override fun timestamp(precision: Int): StaticType = TimestampType(precision)

    override fun clob(): StaticType = StaticType.CLOB

    override fun blob(): StaticType = StaticType.BLOB

    override fun array(): StaticType = ListType(StaticType.ANY)

    override fun array(element: StaticType): StaticType = ListType(element)

    override fun array(element: StaticType, size: Int): StaticType {
        error("StaticTypes does not support length-constrained arrays")
    }

    override fun bag(): StaticType = BagType(StaticType.ANY)

    override fun bag(element: StaticType): StaticType = BagType(element)

    override fun bag(element: StaticType, size: Int): StaticType {
        error("StaticTypes does not support length-constrained bags")
    }

    override fun row(attributes: List<Pair<String, StaticType>>): StaticType {
        val seen = mutableSetOf<String>()
        val fields = attributes.map {
            val attr = it.first
            val type = it.second
            if (seen.contains(attr)) {
                error("row cannot contain duplicate attributes, `$attr`")
            } else {
                seen.add(attr)
            }
            StructType.Field(attr, type)
        }
        return StructType(
            fields = fields,
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.Ordered,
                TupleConstraint.Open(false),
                TupleConstraint.UniqueAttrs(true),
            )
        )
    }

    override fun row(vararg attributes: Pair<String, StaticType>): StaticType = row(attributes.toList())

    override fun struct(): StaticType {
        return StructType(
            fields = emptyList(),
            contentClosed = false,
            constraints = setOf(TupleConstraint.Open(true))
        )
    }

    override fun struct(attributes: Collection<Pair<String, StaticType>>): StaticType {
        var uniq = true
        val seen = mutableSetOf<String>()
        val fields = attributes.map {
            val attr = it.first
            val type = it.second
            if (seen.contains(attr)) {
                uniq = false
            }
            seen.add(attr)
            StructType.Field(attr, type)
        }
        return StructType(
            fields = fields,
            contentClosed = false,
            constraints = setOf(
                TupleConstraint.Open(true),
                TupleConstraint.UniqueAttrs(uniq),
            )
        )
    }

    override fun struct(vararg attributes: Pair<String, StaticType>): StaticType = struct(attributes.toList())
}
