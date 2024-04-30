package org.partiql.cli.io

import org.partiql.value.PartiQLCursor
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import kotlin.math.abs

/**
 * Aids in appending [PartiQLCursor] to [out].
 *
 * Prints in a human-readable fashion. Indents appropriately. Example output:
 * ```
 * partiql ▶ SELECT VALUE { 'a': { 'b': t } } FROM <<1, 2>> AS t
 *    |
 * <<
 *   {
 *     'a': {
 *       'b': 1
 *     }
 *   },
 *   {
 *     'a': {
 *       'b': 2
 *     }
 *   }
 * >>
 * ```
 *
 * @param debug specifies whether to also output typing information; if set to true, values will have their types prefixed
 * to the output; for example: `int32::512`, `string::'hello, world!'`, and `null.int64`; if set to false, values will
 * be printed as-is; for example: `512`, `'hello, world!'`, and `null`.
 */
class PartiQLCursorWriter(
    private val out: Appendable,
    private val debug: Boolean = false
) {

    /**
     * Determines how much to indent
     */
    private var indent = 0

    @OptIn(PartiQLValueExperimental::class)
    fun append(data: PartiQLCursor) {
        for (element in data) {
            writeValue(data, element)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun writeValue(data: PartiQLCursor, element: PartiQLValueType) {
        when (element) {
            PartiQLValueType.ANY -> error("This shouldn't have happened.")
            PartiQLValueType.BOOL -> writeScalar(data, "bool", PartiQLCursor::getBoolValue)
            PartiQLValueType.INT8 -> writeScalar(data, "int8", PartiQLCursor::getInt8Value)
            PartiQLValueType.INT16 -> writeScalar(data, "int16", PartiQLCursor::getInt16Value)
            PartiQLValueType.INT32 -> writeScalar(data, "int32", PartiQLCursor::getInt32Value)
            PartiQLValueType.INT64 -> writeScalar(data, "int64", PartiQLCursor::getInt64Value)
            PartiQLValueType.INT -> writeScalar(data, "int", PartiQLCursor::getIntValue)
            PartiQLValueType.DECIMAL -> writeScalar(data, "decimal", PartiQLCursor::getDecimalValue)
            PartiQLValueType.DECIMAL_ARBITRARY -> writeScalar(data, "decimal_arbitrary", PartiQLCursor::getDecimalArbitraryValue)
            PartiQLValueType.FLOAT32 -> writeScalar(data, "float32", PartiQLCursor::getFloat32Value)
            PartiQLValueType.FLOAT64 -> writeScalar(data, "float64", PartiQLCursor::getFloat64Value)
            PartiQLValueType.CHAR -> writeScalar(data, "char", PartiQLCursor::getCharValue)
            PartiQLValueType.STRING -> writeScalar(data, "string", PartiQLCursor::getStringValue)
            PartiQLValueType.SYMBOL -> writeScalar(data, "symbol", PartiQLCursor::getSymbolValue)
            PartiQLValueType.BINARY -> writeScalar(data, "binary", PartiQLCursor::getBinaryValue)
            PartiQLValueType.BYTE -> writeScalar(data, "byte", PartiQLCursor::getByteValue)
            PartiQLValueType.BLOB -> writeScalar(data, "blob", PartiQLCursor::getBlobValue)
            PartiQLValueType.CLOB -> writeScalar(data, "clob", PartiQLCursor::getClobValue)
            PartiQLValueType.DATE -> writeScalar(data, "date") { it.dateValue.getLiteralString() }
            PartiQLValueType.TIME -> writeScalar(data, "time") { it.timeValue.getLiteralString() }
            PartiQLValueType.TIMESTAMP -> writeScalar(data, "timestamp") { it.timestampValue.getLiteralString() }
            PartiQLValueType.INTERVAL -> writeScalar(data, "interval", PartiQLCursor::getIntervalValue)
            PartiQLValueType.BAG -> writeCollection(data, "bag", "<<", ">>", named = false)
            PartiQLValueType.LIST -> writeCollection(data, "list", "[", "]", named = false)
            PartiQLValueType.SEXP -> writeCollection(data, "sexp", "(", ")", named = false)
            PartiQLValueType.STRUCT -> writeCollection(data, "struct", "{", "}", named = true)
            PartiQLValueType.NULL -> writeScalar(data, "null") { d -> "null".also { assert(d.isNullValue) } }
            PartiQLValueType.MISSING -> writeScalar(data, "missing") { d -> "missing".also { assert(d.isMissingValue) } }
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun writeCollection(data: PartiQLCursor, type: String, prefix: String, postfix: String, named: Boolean) {
        if (appendPotentialNullValue(data, type)) {
            return
        }
        // Print value prefix (AKA: << for bag, [ for list, or ( for s-exp)
        appendTypePrefixIfDebugEnabled(type)
        out.appendLine(prefix)

        // Print children
        stepIn(data)
        for (child in data) {
            out.append(buildIndent())
            if (named) {
                val fieldName = data.fieldName
                out.append('\'')
                out.append(fieldName)
                out.append('\'')
                out.append(": ")
            }
            writeValue(data, child)
            when (data.hasNext()) {
                true -> out.appendLine(",")
                false -> out.appendLine()
            }
        }
        stepOut(data)

        // Print value postfix
        out.append(buildIndent())
        out.append(postfix)
    }

    private fun writeScalar(data: PartiQLCursor, type: String, transform: (PartiQLCursor) -> Any) {
        if (appendPotentialNullValue(data, type)) {
            return
        }
        appendTypePrefixIfDebugEnabled(type)
        out.append(transform(data).toString())
    }

    private fun appendTypePrefixIfDebugEnabled(type: String) {
        if (debug) {
            out.append(type)
            out.append("::")
        }
    }

    /**
     * @return true if the value was null and [out] was appended to; false if the value was not null and [out] was
     * not appended to.
     */
    private fun appendPotentialNullValue(data: PartiQLCursor, type: String): Boolean {
        if (data.isNullValue) {
            out.append("null")
            // Print out the type of the null. AKA: null.bag
            if (debug) {
                out.append('.')
                out.append(type)
            }
            return true
        }
        return false
    }

    private fun stepIn(data: PartiQLCursor) {
        data.stepIn()
        indent++
    }

    private fun stepOut(data: PartiQLCursor) {
        data.stepOut()
        indent--
    }

    private fun buildIndent(): String {
        var prefix = ""
        for (i in 1..indent) {
            prefix += "  "
        }
        return prefix
    }

    private fun Time.getLiteralString(): String {
        val tz = this.timeZone.getTypeString()
        return "TIME $tz '${this.hour}:${this.minute}:${this.decimalSecond}'"
    }

    private fun Date.getLiteralString(): String {
        val dateString = "${this.year.pad(4)}-${this.month.pad()}-${this.day.pad()}"
        return "DATE '$dateString'"
    }

    private fun Timestamp.getLiteralString(): String {
        val tz = this.timeZone.getTypeString()
        val dateString = "${this.year.pad(4)}-${this.month.pad()}-${this.day.pad()}"
        val timeString = "${this.hour.pad()}:${this.minute.pad()}:${this.decimalSecond}"
        val tzLiteral = this.timeZone.getLiteralTimeZoneString()
        return "TIMESTAMP $tz '$dateString $timeString$tzLiteral'"
    }

    private fun TimeZone?.getTypeString() = when (this) {
        null -> "WITHOUT TIME ZONE"
        is TimeZone.UnknownTimeZone -> "WITH UNKNOWN TIME ZONE"
        is TimeZone.UtcOffset -> "WITH TIME ZONE"
    }

    private fun TimeZone?.getLiteralTimeZoneString() = when (this) {
        null -> ""
        is TimeZone.UnknownTimeZone -> "-00:00"
        is TimeZone.UtcOffset -> {
            val sign = when (this.totalOffsetMinutes >= 0) {
                true -> "+"
                false -> "-"
            }
            val offset = abs(this.totalOffsetMinutes)
            val hours = offset.div(60)
            val minutes = offset - (hours * 60)
            "$sign${hours.pad()}:${minutes.pad()}"
        }
    }

    private fun Int.pad(length: Int = 2): String {
        return this.toString().padStart(length, '0')
    }
}
