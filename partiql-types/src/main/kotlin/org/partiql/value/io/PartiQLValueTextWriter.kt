/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.io

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
import org.partiql.value.NullableBagValue
import org.partiql.value.NullableBoolValue
import org.partiql.value.NullableCharValue
import org.partiql.value.NullableDecimalValue
import org.partiql.value.NullableFloat32Value
import org.partiql.value.NullableFloat64Value
import org.partiql.value.NullableInt16Value
import org.partiql.value.NullableInt32Value
import org.partiql.value.NullableInt64Value
import org.partiql.value.NullableInt8Value
import org.partiql.value.NullableIntValue
import org.partiql.value.NullableListValue
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableStructValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.boolValue
import org.partiql.value.charValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue
import org.partiql.value.util.PartiQLValueBaseVisitor
import java.io.PrintStream

/**
 * [PartiQLValueWriter] which outputs PartiQL text.
 *
 * @property out        PrintStream
 * @property formatted  Print with newlines and indents
 * @property indent     Indent prefix, default is 2-spaces
 */
@PartiQLValueExperimental
internal class PartiQLValueTextWriter(
    private val out: PrintStream,
    private val formatted: Boolean = true,
    private val indent: String = "  ",
) : PartiQLValueWriter {

    override fun append(value: PartiQLValue): PartiQLValueWriter {
        val format = if (formatted) Format(indent) else null
        val v = value.accept(ToString, format) // value.toString(format)
        out.append(v)
        return this
    }

    override fun close() {
        out.close()
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

        override fun visitNullableBool(v: NullableBoolValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitBool(boolValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableInt8(v: NullableInt8Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitInt8(int8Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableInt16(v: NullableInt16Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitInt16(int16Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableInt32(v: NullableInt32Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitInt32(int32Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableInt64(v: NullableInt64Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitInt64(int64Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableInt(v: NullableIntValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitInt(intValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableDecimal(v: NullableDecimalValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitDecimal(decimalValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableFloat32(v: NullableFloat32Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitFloat32(float32Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableFloat64(v: NullableFloat64Value, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitFloat64(float64Value(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableChar(v: NullableCharValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitChar(charValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableString(v: NullableStringValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitString(stringValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableSymbol(v: NullableSymbolValue, ctx: Format?) = when (v.value) {
            null -> "null"
            else -> visitSymbol(symbolValue(v.value!!, v.annotations), ctx)
        }

        override fun visitNullableBag(v: NullableBagValue<*>, ctx: Format?) = when (v.isNull()) {
            true -> "null"
            else -> visitBag(v.promote(), ctx)
        }

        override fun visitNullableList(v: NullableListValue<*>, ctx: Format?) = when (v.isNull()) {
            true -> "null"
            else -> visitList(v.promote(), ctx)
        }

        override fun visitNullableSexp(v: NullableSexpValue<*>, ctx: Format?) = when (v.isNull()) {
            true -> "null"
            else -> visitSexp(v.promote(), ctx)
        }

        override fun visitNullableStruct(v: NullableStructValue<*>, ctx: Format?) = when (v.isNull()) {
            true -> "null"
            else -> visitStruct(v.promote(), ctx)
        }
    }
}
