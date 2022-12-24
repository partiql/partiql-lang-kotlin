package org.partiql.isl.internal

import com.amazon.ion.IonType
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.emptyIonStruct
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.isl.Bounds
import org.partiql.isl.Constraint
import org.partiql.isl.Definition
import org.partiql.isl.Flag
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.Import
import org.partiql.isl.IonSchemaNode
import org.partiql.isl.Measure
import org.partiql.isl.Occurs
import org.partiql.isl.Range
import org.partiql.isl.Schema
import org.partiql.isl.Type
import org.partiql.isl.UserReservedFields
import org.partiql.isl.Value
import org.partiql.isl.Version
import org.partiql.isl.visitor.IonSchemaVisitor

/**
 * Produces a sequence of Ion values which represent the given IonSchema
 */
object IonSchemaWriter {

    fun toIon(schema: Schema): Iterator<IonElement> {
        val document = mutableListOf<IonElement>()
        document.apply {
            // Version Marker
            when (schema.version) {
                Version.V1_0 -> {}
                Version.V2_0 -> add(ionSymbol("\$ion_schema_2_0"))
            }
            // Header
            if (schema.header != null) add(Visitor.visit(schema.header, null))
            // Type Definitions
            schema.definitions.forEach { add(Visitor.visit(it, null)) }
            // Footer
            if (schema.footer != null) add(Visitor.visit(schema.footer, null))
        }
        return document.iterator()
    }

    private object Visitor : IonSchemaVisitor<IonElement, Unit>() {

        private val min = ionSymbol("min")
        private val max = ionSymbol("max")

        override fun visit(node: Header, ctx: Unit?) = struct("schema_header") {
            fields(
                "imports" to visit(node.imports),
                "user_reserved_fields" to visit(node.userReservedFields, null)
            )
        }

        override fun visit(node: UserReservedFields, ctx: Unit?) = struct("user_reserved_fields") {
            fields(
                "schema_header" to node.header.symbols(),
                "type" to node.type.symbols(),
                "schema_footer" to node.footer.symbols(),
            )
        }

        override fun visit(node: Footer, ctx: Unit?) = emptyStruct("schema_footer")

        override fun visit(node: Import.Schema, ctx: Unit?) = ionStructOf("id" to ionString(node.id))

        override fun visit(node: Import.Type, ctx: Unit?) = ionStructOf(
            "id" to ionString(node.id),
            "type" to ionSymbol(node.type),
        )

        override fun visit(node: Import.TypeAlias, ctx: Unit?) = ionStructOf(
            "id" to ionString(node.id),
            "type" to ionSymbol(node.type),
            "as" to ionSymbol(node.alias),
        )

        override fun visit(node: Definition, ctx: Unit?) = struct("type") {
            val constraints = node.constraints.map { it.name() to it.accept(this, null)!! }
            fields(
                "name" to ionSymbol(node.name),
                *constraints.toTypedArray(),
            )
        }

        override fun visit(node: Type.Ref, ctx: Unit?): IonElement {
            val ref = ionSymbol(node.name)
            if (node.nullable) {
                return try {
                    IonType.valueOf(node.name.toUpperCase())
                    ionSymbol("\$" + node.name)
                } catch (ex: IllegalArgumentException) {
                    ref.withAnnotations("\$null_or")
                }
            }
            return ref
        }

        override fun visit(node: Type.Inline, ctx: Unit?): IonElement {
            val fields = mutableListOf<StructField>()
            node.constraints.forEach {
                val k = it.name()
                val v = it.accept(this, null)!!
                fields.add(field(k, v))
            }
            if (node.occurs != null) {
                fields.add(field("occurs", visit(node.occurs, null)!!))
            }
            val ref = ionStructOf(fields)
            return if (node.nullable) ref.withAnnotations("\$null_or") else ref
        }

        override fun visit(node: Type.Import, ctx: Unit?): IonElement {
            val fields = mutableListOf(
                field("id", ionSymbol(node.schema)),
                field("type", ionSymbol(node.type)),
            )
            val ref = ionStructOf(fields)
            return if (node.nullable) ref.withAnnotations("\$null_or") else ref
        }

        override fun visit(node: Constraint.AllOf, ctx: Unit?) = visit(node.types)

        override fun visit(node: Constraint.AnyOf, ctx: Unit?) = visit(node.types)

        override fun visit(node: Constraint.Annotations.Values, ctx: Unit?): IonElement {
            val values = node.values.symbols()
            val modifier = node.modifier.name.toLowerCase()
            return values.withAnnotations(modifier)
        }

        override fun visit(node: Constraint.Annotations.Type, ctx: Unit?) = visit(node.type, null)

        override fun visit(node: Constraint.Length.Equals, ctx: Unit?) = ionInt(node.length)

        override fun visit(node: Constraint.Length.Range, ctx: Unit?) = visit(node.range, null)

        override fun visit(node: Constraint.Contains, ctx: Unit?) = visit(node.values)

        override fun visit(node: Constraint.Element, ctx: Unit?): IonElement {
            val ref = visit(node.type, null)!!
            return when (val distinct = node.distinct ?: false) {
                distinct -> ref.withAnnotations("distinct")
                else -> ref
            }
        }

        override fun visit(node: Constraint.Exponent.Equals, ctx: Unit?) = ionInt(node.value)

        override fun visit(node: Constraint.Exponent.Range, ctx: Unit?) = visit(node.range, null)

        override fun visit(node: Constraint.FieldNames, ctx: Unit?): IonElement {
            val ref = visit(node.type, null)!!
            return when (val distinct = node.distinct ?: false) {
                distinct -> ref.withAnnotations("distinct")
                else -> ref
            }
        }

        override fun visit(node: Constraint.Fields, ctx: Unit?): IonElement {
            val fields = ionStructOf(*node.fields.map { it.key to visit(it.value, null)!! }.toTypedArray())
            return when (val closed = node.closed ?: false) {
                closed -> fields.withAnnotations("closed")
                else -> fields
            }
        }

        override fun visit(node: Constraint.Ieee754Float, ctx: Unit?) = when (node.format) {
            Constraint.Ieee754Float.Format.BINARY_16 -> ionSymbol("binary16")
            Constraint.Ieee754Float.Format.BINARY_32 -> ionSymbol("binary32")
            Constraint.Ieee754Float.Format.BINARY_64 -> ionSymbol("binary64")
        }

        override fun visit(node: Constraint.Not, ctx: Unit?) = visit(node.type, null)

        override fun visit(node: Constraint.OneOf, ctx: Unit?) = ionListOf(visit(node.types))

        override fun visit(node: Constraint.OrderedElements, ctx: Unit?) = ionListOf(visit(node.types))

        override fun visit(node: Constraint.Precision.Equals, ctx: Unit?) = ionInt(node.value)

        override fun visit(node: Constraint.Precision.Range, ctx: Unit?) = visit(node.range, null)

        override fun visit(node: Constraint.Regex, ctx: Unit?): IonElement {
            val regex = ionString(node.pattern)
            val annotations = node.flags.map {
                when (it) {
                    Flag.MULTILINE -> "m"
                    Flag.CASE_INSENSITIVE -> "i"
                }
            }
            return regex.withAnnotations(annotations)
        }

        override fun visit(node: Constraint.TimestampOffset, ctx: Unit?) = node.offsets.strings()

        override fun visit(node: Constraint.TimestampPrecision.Equals, ctx: Unit?) =
            ionSymbol(node.value.name.toLowerCase())

        override fun visit(node: Constraint.TimestampPrecision.Range, ctx: Unit?) = visit(node.range, null)

        override fun visit(node: Constraint.Type, ctx: Unit?) = visit(node.type, null)

        override fun visit(node: Constraint.ValidValues, ctx: Unit?) = visit(node.values)

        override fun visit(node: Value.Ion, ctx: Unit?) = node.value

        override fun visit(node: Value.Range, ctx: Unit?) = visit(node.value, null)

        override fun visit(node: Range.Int, ctx: Unit?) = range(
            lower = node.lower?.let { ionInt(it) },
            upper = node.upper?.let { ionInt(it) },
            bounds = node.bounds,
        )

        override fun visit(node: Range.Number, ctx: Unit?) = range(
            lower = node.lower?.let { ionFloat(it) },
            upper = node.upper?.let { ionFloat(it) },
            bounds = node.bounds,
        )

        override fun visit(node: Range.Timestamp, ctx: Unit?) = range(
            lower = node.lower,
            upper = node.upper,
            bounds = node.bounds,
        )

        override fun visit(node: Range.TimestampPrecision, ctx: Unit?) = range(
            lower = node.lower?.let { ionSymbol(it.name.toLowerCase()) },
            upper = node.upper?.let { ionSymbol(it.name.toLowerCase()) },
            bounds = node.bounds,
        )

        override fun visit(node: Occurs.Equal, ctx: Unit?) = ionInt(node.value)

        override fun visit(node: Occurs.Range, ctx: Unit?) = visit(node.range, null)

        override fun visit(node: Occurs.Optional, ctx: Unit?) = ionSymbol("optional")

        override fun visit(node: Occurs.Required, ctx: Unit?) = ionSymbol("required")

        // --- Helpers ---------------------------------------

        private fun visit(nodes: List<IonSchemaNode>) = ionListOf(nodes.map { it.accept(this, null)!! })

        private fun List<String>.symbols() = ionListOf(map { ionSymbol(it) })

        private fun List<String>.strings() = ionListOf(map { ionString(it) })

        private fun struct(id: String, fields: () -> Array<out Pair<String, IonElement>>) =
            ionStructOf(*fields()).withAnnotations(id)

        private fun emptyStruct(id: String) = emptyIonStruct().withAnnotations(id)

        private fun fields(vararg fields: Pair<String, IonElement>) = fields

        private fun Constraint.name(): String = when (this) {
            is Constraint.AllOf -> "all_of"
            is Constraint.Annotations -> "annotations"
            is Constraint.AnyOf -> "any_of"
            is Constraint.Contains -> "contains"
            is Constraint.Element -> "element"
            is Constraint.Exponent.Equals -> "equals"
            is Constraint.Exponent.Range -> "range"
            is Constraint.FieldNames -> "field_names"
            is Constraint.Fields -> "fields"
            is Constraint.Ieee754Float -> "ieee754_float"
            is Constraint.Length.Equals -> {
                when (measure) {
                    Measure.BYTES -> "byte_length"
                    Measure.CODEPOINTS -> "codepoint_length"
                    Measure.ELEMENTS -> "container_length"
                    Measure.UTF8 -> "utf8_byte_length"
                }
            }
            is Constraint.Length.Range -> {
                when (measure) {
                    Measure.BYTES -> "byte_length"
                    Measure.CODEPOINTS -> "codepoint_length"
                    Measure.ELEMENTS -> "container_length"
                    Measure.UTF8 -> "utf8_byte_length"
                }
            }
            is Constraint.Not -> "not"
            is Constraint.OneOf -> "one_of"
            is Constraint.OrderedElements -> "ordered_elements"
            is Constraint.Precision.Equals,
            is Constraint.Precision.Range -> "precision"
            is Constraint.Regex -> "regex"
            is Constraint.TimestampOffset -> "timestamp_offset"
            is Constraint.TimestampPrecision.Equals,
            is Constraint.TimestampPrecision.Range -> "timestamp_precision"
            is Constraint.Type -> "type"
            is Constraint.ValidValues -> "valid_values"
        }

        private fun range(lower: IonElement?, upper: IonElement?, bounds: Bounds): ListElement {
            val l = lower ?: min
            val u = upper ?: max
            val lx = l.withAnnotations("exclusive")
            val ux = u.withAnnotations("exclusive")
            val range = when (bounds) {
                Bounds.INCLUSIVE -> l to u
                Bounds.EXCLUSIVE -> lx to ux
                Bounds.L_EXCLUSIVE -> lx to u
                Bounds.R_EXCLUSIVE -> l to ux
            }
            return ionListOf(range.first, range.second).withAnnotations("range")
        }
    }
}
