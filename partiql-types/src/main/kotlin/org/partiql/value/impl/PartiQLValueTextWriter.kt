package org.partiql.value.impl

import org.partiql.value.AnyValue
import org.partiql.value.BagValue
import org.partiql.value.BoolValue
import org.partiql.value.CharValue
import org.partiql.value.CollectionValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueBaseVisitor
import org.partiql.value.PartiQLValueWriter
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import java.io.PrintStream

/**
 * [PartiQLValueWriter] which outputs PartiQL text.
 *
 * @property out        PrintStream
 * @property formatted  Print with newlines and indents
 * @property indent     Indent prefix, default is 2-spaces
 */
internal class PartiQLValueTextWriter(
    private val out: PrintStream,
    private val formatted: Boolean = true,
    private val indent: String = "  ",
) : PartiQLValueWriter {

    override fun writeValue(value: PartiQLValue) {
        val format = if (formatted) Format(indent) else null
        val v = value.accept(ToString, format) // value.toString(format)
        out.append(v)
    }

    override fun writeValues(values: Iterator<PartiQLValue>) {
        val format = if (formatted) Format(indent) else null
        values.forEach {
            val v = it.accept(ToString, format) // value.toString(format)
            out.appendLine(v)
        }
    }

    /**
     * Text format
     *
     * @param indent    Index prefix
     */
    private data class Format(
        val indent: String = "  ",
        val prefix: String = "",
    ) {
        fun nest() = copy(prefix = prefix + indent)
    }

    /**
     * Not implemented on the value classes as the textual format is not inherent to the values.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToString : PartiQLValueBaseVisitor<String, Format?>() {

        override fun defaultVisit(v: PartiQLValue, format: Format?) = defaultReturn(v, format)

        override fun defaultReturn(v: PartiQLValue, format: Format?): Nothing =
            throw IllegalArgumentException("Cannot write value of type ${v.type}")

        private inline fun PartiQLValue.toString(format: Format?, block: PartiQLValue.() -> String) = buildString {
            if (format != null) append(format.prefix)
            annotate(this@toString, this)
            append(block())
        }

        private fun annotate(v: PartiQLValue, sb: StringBuilder) {
            val annotations = v.annotations
            if (annotations.isNotEmpty()) {
                // handle escaping
                sb.append(annotations.joinToString("::", postfix = "::"))
            }
        }

        private fun visit(
            v: CollectionValue<*>,
            format: Format?,
            terminals: Pair<String, String>,
            separator: CharSequence = ",",
        ) = buildString {
            // skip empty
            if (v.isEmpty() || format == null) {
                format?.let { append(it.prefix) }
                annotate(v, this)
                append(terminals.first)
                val items = v.elements.map {
                    it.accept(ToString, null) // it.toString()
                }
                append(items.joinToString(separator))
                append(terminals.second)
                return@buildString
            }
            // print formatted
            append(format.prefix)
            annotate(v, this)
            appendLine(terminals.first)
            v.elements.forEachIndexed { i, e ->
                val content = e.accept(ToString, format.nest()) // e.toString(format)
                val suffix = if (i == v.size - 1) "" else ","
                append(content)
                appendLine(suffix)
            }
            append(format.prefix)
            append(terminals.second)
        }

        override fun visitNull(v: NullValue, format: Format?) = v.toString(format) { "null" }

        override fun visitMissing(v: MissingValue, format: Format?) = v.toString(format) { "missing" }

        override fun visitBool(v: BoolValue, format: Format?) = v.toString(format) {
            when (v.value) {
                true -> "true"
                else -> "false"
            }
        }

        override fun visitInt8(v: Int8Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitInt16(v: Int16Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitInt32(v: Int32Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitInt64(v: Int64Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitInt(v: IntValue, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitDecimal(v: DecimalValue, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitFloat32(v: Float32Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitFloat64(v: Float64Value, format: Format?) = v.toString(format) { v.value.toString() }

        override fun visitChar(v: CharValue, format: Format?) = v.toString(format) { "'${v.value}'" }

        // TODO escapes
        override fun visitString(v: StringValue, format: Format?) = v.toString(format) { "'${v.value}'" }

        // TODO escapes
        override fun visitSymbol(v: SymbolValue, format: Format?) = v.toString(format) { v.value }

        override fun visitBag(v: BagValue<*>, format: Format?) = visit(v, format, "<<" to ">>")

        override fun visitList(v: ListValue<*>, format: Format?) = visit(v, format, "[" to "]")

        override fun visitSexp(v: SexpValue<*>, format: Format?) = visit(v, format, "(" to ")", " ")

        override fun visitStruct(v: StructValue<*>, format: Format?): String = buildString {
            if (v.isEmpty() || format == null) {
                format?.let { append(it.prefix) }
                annotate(v, this)
                append("{")
                val items = v.fields.map {
                    val fk = it.first
                    val fv = it.second.accept(ToString, null) // it.toString()
                    "$fk:$fv"
                }
                append(items.joinToString(","))
                append("}")
                return@buildString
            }
            // print formatted
            append(format.prefix)
            annotate(v, this)
            appendLine("{")
            v.fields.forEachIndexed { i, e ->
                val fk = e.first
                val fv = e.second.accept(ToString, format.nest()).trimStart() // e.toString(format)
                val suffix = if (i == v.size - 1) "" else ","
                append(format.prefix + format.indent)
                append("$fk: $fv")
                appendLine(suffix)
            }
            append(format.prefix)
            append("}")
        }

        override fun visitAny(v: AnyValue, format: Format?) = v.toString(format) { "any" }
    }
}
