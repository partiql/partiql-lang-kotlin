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

package org.partiql.spi.value.io

import org.partiql.value.BagValue
import org.partiql.value.BoolValue
import org.partiql.value.CharValue
import org.partiql.value.CollectionValue
import org.partiql.value.DateValue
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
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import org.partiql.value.util.PartiQLValueBaseVisitor
import java.io.OutputStream
import java.io.PrintStream
import java.math.BigDecimal
import kotlin.math.abs

/**
 * [PartiQLValueWriter] which outputs PartiQL text.
 *
 * @property out        PrintStream
 * @property formatted  Print with newlines and indents
 * @property indent     Indent prefix, default is 2-spaces
 */
public class PartiQLValueTextWriter(
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
                // TODO handle escaping
                sb.append(annotations.joinToString("::", postfix = "::"))
            }
        }

        override fun visitNull(v: NullValue, format: Format?) = v.toString(format) { "null" }

        override fun visitMissing(v: MissingValue, format: Format?) = v.toString(format) { "missing" }

        override fun visitBool(v: BoolValue, format: Format?) = v.toString(format) {
            when (v.value) {
                null -> "null"
                true -> "true"
                false -> "false"
            }
        }

        override fun visitInt8(v: Int8Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.int8
                else -> value.toString()
            }
        }

        override fun visitInt16(v: Int16Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.int16
                else -> value.toString()
            }
        }

        override fun visitInt32(v: Int32Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.int32
                else -> value.toString()
            }
        }

        override fun visitInt64(v: Int64Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.int64
                else -> value.toString()
            }
        }

        override fun visitInt(v: IntValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.int
                else -> value.toString()
            }
        }

        override fun visitDecimal(v: DecimalValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.decimal
                else -> value.toString()
            }
        }

        override fun visitFloat32(v: Float32Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.float32
                else -> value.toString()
            }
        }

        override fun visitFloat64(v: Float64Value, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.float64
                else -> value.toString()
            }
        }

        override fun visitChar(v: CharValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.char
                else -> "'$value'"
            }
        }

        // TODO escapes
        override fun visitString(v: StringValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.string
                else -> "'$value'"
            }
        }

        // TODO escapes, find source in IonJava
        override fun visitSymbol(v: SymbolValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.symbol
                else -> value
            }
        }

        override fun visitDate(v: DateValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.date
                else -> sqlString(value)
            }
        }

        private fun padZeros(v: Int, totalDigits: Int): String = String.format("%0${totalDigits}d", v)

        private fun sqlString(d: Date): String {
            val yyyy = padZeros(d.year, 4)
            val mm = padZeros(d.month, 2)
            val dd = padZeros(d.day, 2)
            return "DATE '$yyyy-$mm-$dd'"
        }

        override fun visitTime(v: TimeValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.time
                else -> sqlString(value)
            }
        }

        private fun sqlString(tz: TimeZone?): String {
            return when (tz) {
                null -> ""
                is TimeZone.UnknownTimeZone -> "-00:00"
                is TimeZone.UtcOffset -> {
                    val sign = if (tz.totalOffsetMinutes < 0) {
                        "-"
                    } else {
                        "+"
                    }
                    val hh = padZeros(abs(tz.tzHour), 2)
                    val mm = padZeros(abs(tz.tzMinute), 2)
                    "$sign$hh:$mm"
                }
            }
        }

        private fun sqlString(t: Time): String {
            val hh = padZeros(t.hour, 2)
            val mm = padZeros(t.minute, 2)
            val ss = padZeros(t.decimalSecond.toInt(), 2)
            val frac = t.decimalSecond.remainder(BigDecimal.ONE).toString().substring(1) // drop leading 0
            val timeZone = sqlString(t.timeZone)
            return "TIME '$hh:$mm:$ss$frac$timeZone'"
        }

        override fun visitTimestamp(v: TimestampValue, format: Format?) = v.toString(format) {
            when (val value = v.value) {
                null -> "null" // null.timestamp
                else -> sqlString(value)
            }
        }

        private fun sqlString(t: Timestamp): String {
            val yyyy = padZeros(t.year, 4)
            val mon = padZeros(t.month, 2)
            val dd = padZeros(t.day, 2)
            val hh = padZeros(t.hour, 2)
            val min = padZeros(t.minute, 2)
            val ss = padZeros(t.decimalSecond.toInt(), 2)
            val frac = t.decimalSecond.remainder(BigDecimal.ONE).toString().substring(1) // drop leading 0
            val timeZone = sqlString(t.timeZone)
            return "TIMESTAMP '$yyyy-$mon-$dd $hh:$min:$ss$frac$timeZone'"
        }

        override fun visitBag(v: BagValue<*>, format: Format?) = collection(v, format, "<<" to ">>")

        override fun visitList(v: ListValue<*>, format: Format?) = collection(v, format, "[" to "]")

        override fun visitSexp(v: SexpValue<*>, format: Format?) = collection(v, format, "(" to ")", " ")

        override fun visitStruct(v: StructValue<*>, format: Format?): String = buildString {
            if (v.isNull) {
                return "null"
            }
            // null.struct
            val entries = v.entries.toList()
            // {}
            if (entries.isEmpty() || format == null) {
                format?.let { append(it.prefix) }
                annotate(v, this)
                append("{")
                val items = entries.map {
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
            entries.forEachIndexed { i, e ->
                val fk = e.first
                val fv = e.second.accept(ToString, format.nest()).trimStart() // e.toString(format)
                val suffix = if (i == entries.size - 1) "" else ","
                append(format.prefix + format.indent)
                append("$fk: $fv")
                appendLine(suffix)
            }
            append(format.prefix)
            append("}")
        }

        private fun collection(
            v: CollectionValue<*>,
            format: Format?,
            terminals: Pair<String, String>,
            separator: CharSequence = ",",
        ) = buildString {
            // null.bag, null.list, null.sexp
            if (v.isNull) {
                return "null"
            }
            val elements = v.toList()
            // skip empty
            if (elements.isEmpty() || format == null) {
                format?.let { append(it.prefix) }
                annotate(v, this)
                append(terminals.first)
                val items = elements.map {
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
            elements.forEachIndexed { i, e ->
                val content = e.accept(ToString, format.nest()) // e.toString(format)
                val suffix = if (i == elements.size - 1) "" else ","
                append(content)
                appendLine(suffix)
            }
            append(format.prefix)
            append(terminals.second)
        }
    }
}

public class PartiQLValueWriterBuilder private constructor() {

    private var formatted: Boolean = true

    public companion object {
        @JvmStatic
        public fun standard(): PartiQLValueWriterBuilder = PartiQLValueWriterBuilder()
    }

    public fun build(outputStream: OutputStream): PartiQLValueWriter =
        PartiQLValueTextWriter(
            out = PrintStream(outputStream),
            formatted = formatted,
        )

    public fun formatted(formatted: Boolean = true): PartiQLValueWriterBuilder =
        this.apply { this.formatted = formatted }
}
